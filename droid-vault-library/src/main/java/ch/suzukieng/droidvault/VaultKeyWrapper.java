package ch.suzukieng.droidvault;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

/**
 * Helper class for protecting the Vault key using AndroidKeyStore provider.
 * Wrapping here means that we protect our secret key with public/private key pair stored in the
 * platform {@link KeyStore}. This protects the symmetric vault key with asymmetric, hardware-backed
 * crypto, if provided by the device.
 * <p/>
 * See <a href="http://en.wikipedia.org/wiki/Key_Wrap">key wrapping</a> for more details.
 * <p/>
 * Not inherently thread safe.
 * <p/>
 * Adapted from: https://android.googlesource.com/platform/development/+/master/samples/Vault/src/com/example/android/vault/SecretKeyWrapper.java
 */
public class VaultKeyWrapper {

    private static final String TAG = VaultKeyWrapper.class.getSimpleName();

    private static final String KEYSTORE_KEY_ALIAS = "DroidVAult";

    private static final String CIPHER_AES = "AES";

    /**
     * The symmetric cipher to use to
     */
    private final Cipher cipher;
    private final KeyPair keyPair;

    /**
     * Create a wrapper using the public/private key pair with the given alias.
     * If no pair with that alias exists, it will be generated.
     *
     * @param context
     * @param keyStoreEncryptionRequired
     */
    public VaultKeyWrapper(Context context, boolean keyStoreEncryptionRequired) throws GeneralSecurityException, IOException {
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEYSTORE_KEY_ALIAS)) {
            generateKeyPair(context, keyStoreEncryptionRequired);
        }
        // Even if we just generated the key, always read it back to ensure we
        // can read it successfully.
        final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEYSTORE_KEY_ALIAS, null);
        keyPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
    }


    /**
     * Delete the vault wrapper key.
     *
     * @throws VaultException Something went wrong during the deletion of the keypair.
     */
    public static void deleteKey() throws VaultException {
        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(KEYSTORE_KEY_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_KEY_ALIAS);
            }
        } catch (KeyStoreException e) {
            Log.e(TAG, "Failed to delete key with alias: " + KEYSTORE_KEY_ALIAS, e);
            throw new VaultException("Failed to delete vault keypair in Android KeyStore", e);
        } catch (CertificateException e) {
            Log.e(TAG, "Failed to delete key with alias: " + KEYSTORE_KEY_ALIAS, e);
            throw new VaultException("Failed to delete vault keypair in Android KeyStore", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to delete key with alias: " + KEYSTORE_KEY_ALIAS, e);
            throw new VaultException("Failed to delete vault keypair in Android KeyStore", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to delete key with alias: " + KEYSTORE_KEY_ALIAS, e);
            throw new VaultException("Failed to delete vault keypair in Android KeyStore", e);
        }
    }

    private static void generateKeyPair(Context context, boolean keyStoreEncryptionRequired) throws GeneralSecurityException {
        final Calendar start = new GregorianCalendar();
        final Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, 100);
        final KeyPairGeneratorSpec.Builder builder = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEYSTORE_KEY_ALIAS)
                .setSubject(new X500Principal("CN=" + KEYSTORE_KEY_ALIAS))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime());
        if (keyStoreEncryptionRequired) {
            builder.setEncryptionRequired();
        }
        final KeyPairGeneratorSpec spec = builder.build();
        final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        gen.initialize(spec);
        gen.generateKeyPair();
    }

    /**
     * Wrap a {@link SecretKey} using the public key assigned to this wrapper.
     * Use {@link #unwrap(byte[])} to later recover the original
     * {@link SecretKey}.
     *
     * @return a wrapped version of the given {@link SecretKey} that can be
     * safely stored on untrusted storage.
     */
    public byte[] wrap(SecretKey key) throws GeneralSecurityException {
        cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
        return cipher.wrap(key);
    }

    /**
     * Unwrap a {@link SecretKey} using the private key assigned to this
     * wrapper.
     *
     * @param blob a wrapped {@link SecretKey} as previously returned by
     *             {@link #wrap(SecretKey)}.
     */
    public SecretKey unwrap(byte[] blob) throws GeneralSecurityException {
        cipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
        return (SecretKey) cipher.unwrap(blob, CIPHER_AES, Cipher.SECRET_KEY);
    }

}