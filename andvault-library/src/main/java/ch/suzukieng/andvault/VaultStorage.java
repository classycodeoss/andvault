package ch.suzukieng.andvault;

import java.util.List;

/**
 * Storage for encrypted credentials and vault key. You probably don't need to care about this as
 * {@link SharedPrefsStorage} should be fine for you.
 * <p/>
 * The storage does not need to have an encryption or obfuscation component, as it only stores
 * encrypted values to begin with.
 *
 * @author Alex Suzuki, Suzuki Engineering GmbH, 2015
 */
public interface VaultStorage {

    /**
     * @return The names of the encrypted credentials in this storage.
     */
    List<String> getCredentialNames();

    /**
     * Return the stored value for the credential with the given name.
     *
     * @param name The name of the credential.
     * @return The value, or null, if the storage does not contain the credential
     */
    byte[] getCredential(String name);

    /**
     * Store the credential.
     *
     * @param name  The name of the credential.
     * @param value The (encrypted) value of the credential
     */
    void setCredential(String name, byte[] value);

    /**
     * Remove the given credential.
     *
     * @param name The name of the credential.
     */
    void removeCredential(String name);

    /**
     * Store the wrapped encryption key. It is safe to store this key, as it is wrapped (encrypted)
     * with a key in the Android Keystore.
     *
     * @param key The wrapped (encrypted) key.
     */
    void setKey(byte[] key);

    /**
     * @return The wrapped encryption key.
     */
    byte[] getKey();

    /**
     * Clear the storage, remove all credentials and destroy the encryption key.
     */
    void reset();
}
