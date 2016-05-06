package com.classycode.andvault;

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

    private static final String KEYSTORE_KEY_ALIAS = "andvault";

    private static final String CIPHER_AES = "AES";

    /**
     * The symmetric cipher to use for the key wrapping.
     */
    private final Cipher cipher;

    /**
     * A reference to the vault master keypair. The private key is never exposed.
     */
    private final KeyPair keyPair;

    /**
     * Create a wrapper using the public/private key pair with the given alias.
     * If no pair with that alias exists, it will be generated.
     *
     * @param context The application context
     * @throws GeneralSecurityException An error occurred while creating or loading the keypair in the Android KeyStore
     */
    public VaultKeyWrapper(Context context) throws GeneralSecurityException {
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        final KeyStore keyStore = getAndLoadKeystore();
        if (!keyStore.containsAlias(KEYSTORE_KEY_ALIAS)) {
            generateKeyPair(context);
        }

        // Even if we just generated the key, always read it back to ensure can read it successfully.
        final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEYSTORE_KEY_ALIAS, null);
        keyPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
    }

    /**
     * Get a reference to the AndroidKeyStore. We assume this always works, as we require Android 4.3+
     *
     * @return The Android KeyStore
     * @throws IllegalStateException If against all odds we can not load the keystore
     */
    private static KeyStore getAndLoadKeystore() {
        try {
            final KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return keyStore;
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Error obtaining AndroidKeyStore", e);
        } catch (CertificateException e) {
            throw new IllegalStateException("Error obtaining AndroidKeyStore", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error obtaining AndroidKeyStore", e);
        } catch (IOException e) {
            throw new IllegalStateException("Error obtaining AndroidKeyStore", e);
        }
    }


    /**
     * Delete the vault master keypair, effectively throwing away the key to the vault.
     */
    public static void deleteKey() {
        try {
            getAndLoadKeystore().deleteEntry(KEYSTORE_KEY_ALIAS);
        } catch (KeyStoreException e) {
            // not sure if it's wise to ignore this, but on the other hand, there isn't that much we can do.
            Log.w(TAG, "Failed to delete entry in AndroidKeyStore, ignoring", e);
        }
    }

    /**
     * Generate the vault master keypair.
     *
     * @param context
     * @throws GeneralSecurityException
     */
    private void generateKeyPair(Context context) throws GeneralSecurityException {
        final Calendar start = new GregorianCalendar();
        final Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, 100);
        final KeyPairGeneratorSpec.Builder builder = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEYSTORE_KEY_ALIAS)
                .setSubject(new X500Principal("CN=" + KEYSTORE_KEY_ALIAS))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .setEncryptionRequired();
        final KeyPairGeneratorSpec spec = builder.build();
        final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        gen.initialize(spec);
        gen.generateKeyPair();
    }

    /**
     * Wrap a {@link SecretKey} using the public key of the vault master keypair.
     * Use {@link #unwrap(byte[])} to later recover the original
     * {@link SecretKey}.
     *
     * @return a wrapped version of the given {@link SecretKey} that can be safely stored on untrusted storage.
     */
    public byte[] wrap(SecretKey key) throws GeneralSecurityException {
        cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
        return cipher.wrap(key);
    }

    /**
     * Unwrap a {@link SecretKey} using the private part of the vault master keypair. The private
     * key remains outside of this process, the cipher is offloaded.
     *
     * @param blob a wrapped {@link SecretKey} as previously returned by {@link #wrap(SecretKey)}.
     */
    public SecretKey unwrap(byte[] blob) throws GeneralSecurityException {
        cipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
        return (SecretKey) cipher.unwrap(blob, CIPHER_AES, Cipher.SECRET_KEY);
    }

}