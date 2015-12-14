package ch.suzukieng.droidvault;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.security.KeyChain;
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

    /**
     * Initialize the Vault with an application {@link Context}. This uses the default storage mechanism {@link SharedPrefsStorage}.
     *
     * @param context The Context, should be an application context as these can be referenced safely without leaking activities.
     */
    public Vault(Context context) {
        this(context, new SharedPrefsStorage(context));
    }

    /**
     * Initialize the Vault with the given {@link Context} and {@link SharedPrefsStorage}. This allows you to use a different
     * storage mechanism than {@link android.content.SharedPreferences}.
     *
     * @param context The Context, should be an application context as these can be referenced safely without leaking activities.
     * @param storage A storage mechanism.
     */
    public Vault(Context context, VaultStorage storage) {
        this.context = context;
        if (!(context instanceof Application)) {
            Log.w(TAG, "Vault initialized with non-application context. You should always use Application Contexts to avoid leaking memory.");
        }
        this.storage = storage;
    }

    private SecretKey getOrCreateVaultKey() throws GeneralSecurityException, IOException {
        final VaultKeyWrapper keyWrapper = new VaultKeyWrapper(context);
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
     */
    public void reset() throws VaultException {
        VaultKeyWrapper.deleteKey();
        storage.reset();
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
    public boolean isHardwareBackedCredentialStorage() {
        return KeyChain.isBoundKeyAlgorithm(KeyProperties.KEY_ALGORITHM_RSA);
    }

    /**
     * @return True if the device is protected by PIN, passcode or pattern. A SIM lock counts as well.
     */
    public boolean isDeviceProtected() {
        final KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }
}
