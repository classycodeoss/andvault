package ch.suzukieng.droidvault;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.security.KeyChain;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secure storage of application secrets, using the Android KeyStore provider.
 *
 * @author Alex Suzuki, Suzuki Engineering GmbH, 2015
 */
public class Vault {

    private static final String TAG = Vault.class.getSimpleName();

    private static final String CIPHER_AES = "AES";

    /**
     * Key length for AES-256
     */
    public static final int KEY_LENGTH = 32;

    private final Context context;

    private final VaultStorage storage;

    private boolean keyStoreEncryptionRequired;

    /**
     * Initialize the Vault with an application {@link Context}. This uses the default storage mechanism {@link SharedPrefsStorage}.
     *
     * @param context The Context, should be an application context as these can be referenced safely without leaking activities.
     * @throws VaultException vault initialization failed, this can happen when the device lock screen method changes, rendering
     *                        the master key in the KeyStore unusable. Usually you need to throw away the vault now using {@link Vault#reset(Context)}.
     */
    public Vault(Context context) throws VaultException {
        this(context, true, new SharedPrefsStorage(context));
    }

    /**
     * Initialize the Vault with an application {@link Context}. This uses the default storage mechanism {@link SharedPrefsStorage}.
     *
     * @param context                    The Context, should be an application context as these can be referenced safely without leaking activities.
     * @param keyStoreEncryptionRequired If the keypair in the Android KeyStore should be encrypted at rest, see
     *                                   {@link KeyPairGeneratorSpec.Builder#setEncryptionRequired()}. This is true by default, and requires the device
     *                                   to be protected by a PIN, passcode or pattern. It is highly recommended that you set this to true for sensitive
     *                                   data.
     * @throws VaultException vault initialization failed, this can happen when the device lock screen method changes, rendering
     *                        the master key in the KeyStore unusable. Usually you need to throw away the vault now using {@link Vault#reset(Context)}.
     */
    public Vault(Context context, boolean keyStoreEncryptionRequired) throws VaultException {
        this(context, keyStoreEncryptionRequired, new SharedPrefsStorage(context));
    }


    /**
     * Initialize the Vault with the given {@link Context} and {@link SharedPrefsStorage}. This allows you to use a different
     * storage mechanism than {@link android.content.SharedPreferences}.
     *
     * @param context                    The Context, should be an application context as these can be referenced safely without leaking activities.
     * @param keyStoreEncryptionRequired If the keypair in the Android KeyStore should be encrypted at rest, see
     *                                   {@link KeyPairGeneratorSpec.Builder#setEncryptionRequired()}. This is true by default, and requires the device
     *                                   to be protected by a PIN, passcode or pattern. It is highly recommended that you set this to true for sensitive
     *                                   data.
     * @param storage                    A storage mechanism.
     * @throws VaultException vault initialization failed, this can happen when the device lock screen method changes, rendering
     *                        the master key in the KeyStore unusable. Usually you need to throw away the vault now using {@link Vault#reset(Context)}.
     */
    public Vault(Context context, boolean keyStoreEncryptionRequired, VaultStorage storage) throws VaultException {
        this.context = context;
        if (!(context instanceof Application)) {
            Log.w(TAG, "Vault initialized with non-application context. You should always use Application Contexts to avoid leaking memory.");
        }
        this.storage = storage;
        this.keyStoreEncryptionRequired = keyStoreEncryptionRequired;
        if (keyStoreEncryptionRequired && !Vault.isDeviceProtected(context)) {
            throw new VaultException("Keypair encryption is requested, but device is not protected. Handle this in your app by using Vault.isDeviceProtected(Context)");
        }

        // early initialization to catch device lock changes.
        try {
            getOrCreateVaultKey();
        } catch (GeneralSecurityException e) {
            throw new VaultException("Initializing the Vault failed (HINT: did device lockscreen setting change?)", e);
        } catch (IOException e) {
            throw new VaultException("Initializing the Vault failed (HINT: did device lockscreen setting change?)", e);
        }
    }

    /**
     * @return True if the master key in the Android KeyStore should be encrypted at rest. This is true by default.
     */
    public boolean isKeyStoreEncryptionRequired() {
        return keyStoreEncryptionRequired;
    }

    private SecretKey getOrCreateVaultKey() throws GeneralSecurityException, IOException {
        final VaultKeyWrapper keyWrapper = new VaultKeyWrapper(context, keyStoreEncryptionRequired);
        final byte[] wrappedVaultKey = storage.getKey();
        if (wrappedVaultKey == null) { // no symmetric key yet, create and random one, and wrap it
            final byte[] raw = new byte[KEY_LENGTH];
            new SecureRandom().nextBytes(raw);
            final SecretKey key = new SecretKeySpec(raw, CIPHER_AES);
            storage.setKey(keyWrapper.wrap(key));
            return key;
        } else {
            return keyWrapper.unwrap(wrappedVaultKey);
        }
    }

    public List<String> getCredentialNames() {
        return storage.getCredentialNames();
    }

    public void removeCredential(String name) {
        storage.removeCredential(name);
    }

    public byte[] getCredential(String name) throws VaultException {
        final byte[] encryptedCredential = storage.getCredential(name);
        if (encryptedCredential == null) {
            return null;
        }
        try {
            return decrypt(encryptedCredential);
        } catch (GeneralSecurityException e) {
            throw new VaultException("Credential could not be read", e);
        } catch (IOException e) {
            throw new VaultException("Credential could not be read", e);
        }
    }

    public String getStringCredential(String name) throws VaultException {
        byte[] value = getCredential(name);
        if (value == null) {
            return null;
        } else {
            try {
                return new String(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e); // should never happen
            }
        }
    }

    public void storeCredential(String name, byte[] value) throws VaultException {
        try {
            storage.setCredential(name, encrypt(value));
        } catch (GeneralSecurityException e) {
            throw new VaultException("Credential could not be stored", e);
        } catch (IOException e) {
            throw new VaultException("Credential could not be stored", e);
        }
    }

    public void storeStringCredential(String name, String value) throws VaultException {
        try {
            storeCredential(name, value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e); // should never happen
        }
    }

    /**
     * Clear the vault, remove all domains and credentials, throw away any key material.
     *
     * @param context The application context
     */
    public static void reset(Context context) {
        try {
            VaultKeyWrapper.deleteKey();
            new SharedPrefsStorage(context).reset();
        } catch (VaultException e) {
            Log.w(TAG, "Ignoring exception while deleting master key", e);
        }
    }

    /**
     * Decrypt a value using the vault key.
     *
     * @param value
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private byte[] decrypt(byte[] value) throws GeneralSecurityException, IOException {
        final Cipher cipher = Cipher.getInstance(CIPHER_AES);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateVaultKey());
        return cipher.doFinal(value);
    }

    /**
     * Encrypt a value using the vault key.
     *
     * @param value
     * @return The encrypted value
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private byte[] encrypt(byte[] value) throws GeneralSecurityException, IOException {
        final Cipher cipher = Cipher.getInstance(CIPHER_AES);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateVaultKey());
        return cipher.doFinal(value);
    }

    /**
     * @return True if the device offers hardware-backed protection of the master key.
     */
    public static boolean isHardwareBackedCredentialStorage() {
        return KeyChain.isBoundKeyAlgorithm(KeyProperties.KEY_ALGORITHM_RSA);
    }

    /**
     * @return True if the device is protected by PIN, passcode or pattern. A SIM lock counts as well.
     */
    public static boolean isDeviceProtected(Context context) {
        final KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }
}
