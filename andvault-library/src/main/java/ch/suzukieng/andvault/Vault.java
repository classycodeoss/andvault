package ch.suzukieng.andvault;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.security.KeyChain;
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

    /**
     * Initialize the Vault with an application {@link Context}. This uses the default storage mechanism {@link SharedPrefsStorage}.
     *
     * @param context The Context, should be an application context as these can be referenced safely without leaking activities.
     * @throws VaultException Thrown if the vault could not be initialized, this can happen if the device is not protected
     *                        (check {@link Vault#isDeviceProtected(Context)}) or if the device lock scheme has changed,
     *                        rendering the keystore unusable. You might need to reset the Vault if this happens using
     *                        {@link Vault#reset(Context)}
     */
    public Vault(Context context) throws VaultException {
        this(context, new SharedPrefsStorage(context));
    }

    /**
     * Initialize the Vault with the given {@link Context} and {@link SharedPrefsStorage}. This allows you to use a different
     * storage mechanism than {@link android.content.SharedPreferences}.
     *
     * @param context The Context, should be an application context as these can be referenced safely without leaking activities.
     * @param storage A storage mechanism.
     * @throws VaultException Thrown if the vault could not be initialized, this can happen if the device is not protected
     *                        (check {@link Vault#isDeviceProtected(Context)}) or if the device lock scheme has changed,
     *                        rendering the keystore unusable. You might need to reset the Vault if this happens using
     *                        {@link Vault#reset(Context)}
     */
    public Vault(Context context, VaultStorage storage) throws VaultException {
        if (!Vault.isDeviceProtected(context)) {
            throw new VaultException("Keypair encryption is requested, but device is not protected. Handle this in your app by using Vault.isDeviceProtected(Context)");
        }

        this.context = context;
        if (!(context instanceof Application)) {
            Log.w(TAG, "Vault initialized with non-application context. You should always use Application Contexts to avoid leaking memory.");
        }
        this.storage = storage;

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
     * @return The (symmetric) vault key. If it does not exist yet, it is created.
     * @throws GeneralSecurityException
     * @throws IOException
     */
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

    /**
     * @return The list of credentials stored in the vault.
     */
    public List<String> getCredentialNames() {
        return storage.getCredentialNames();
    }

    /**
     * Remove the named credential from the vault.
     *
     * @param name The credential's name
     */
    public void removeCredential(String name) {
        storage.removeCredential(name);
    }

    /**
     * Get the value of the named credential.
     *
     * @param name The credential's name (must not be null)
     * @return The credential value, or null, if the credential does not exist in the vault
     * @throws VaultException An error occurred while retrieving the credential from the vault
     */
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

    /**
     * Get the value of the named credential, which is assumed to be a string.
     *
     * @param name The credential's name (must not be null)
     * @return The credential value as a string, or null, if the credential does not exist in the vault
     * @throws VaultException An error occurred while retrieving the credential from the vault
     */
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

    /**
     * Store a new credential, which is assumed to be a string, in the vault or overwrite an existing one.
     *
     * @param name  The credential's name (must not be null)
     * @param value The credential's value (must not be null)
     * @throws VaultException An error occurred while storing the credential
     */
    public void storeCredential(String name, byte[] value) throws VaultException {
        try {
            storage.setCredential(name, encrypt(value));
        } catch (GeneralSecurityException e) {
            throw new VaultException("Credential could not be stored", e);
        } catch (IOException e) {
            throw new VaultException("Credential could not be stored", e);
        }
    }

    /**
     * Store a new credential in the vault or overwrite an existing one.
     *
     * @param name  The credential's name (must not be null)
     * @param value The credential's value (must not be null)
     * @throws VaultException An error occurred while storing the credential
     */
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
        VaultKeyWrapper.deleteKey();
        new SharedPrefsStorage(context).reset();
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
        return KeyChain.isBoundKeyAlgorithm("RSA");
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
