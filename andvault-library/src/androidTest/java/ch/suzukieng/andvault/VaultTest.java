package ch.suzukieng.andvault;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.RequiresDevice;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;

/**
 * @author Alex Suzuki, Suzuki Engineering GmbH, 2015
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
@RequiresDevice // actually, requires a lock screen. restrict to real device as we currently can't set this up on the CI server
public class VaultTest {

    private Context context;

    @Before
    public void setup() throws VaultException {
        context = new RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "VaultKeyWrapperTest");
        Vault.reset(context);
    }

    @Test
    public void testVaultReset() throws VaultException {
        Vault vault = new Vault(context);
        Assert.assertEquals(0, vault.getCredentialNames().size());
    }

    @Test
    public void testInexistentCredentialGetReturnsNull() throws VaultException {
        Vault vault = new Vault(context);
        Assert.assertNull(vault.getCredential("inexistent"));
    }

    @Test
    public void testStoreCredential() throws VaultException, UnsupportedEncodingException {
        byte[] value = "some value".getBytes("UTF-8");
        Vault vault = new Vault(context);
        vault.storeCredential("cred", value);
        Assert.assertTrue(vault.getCredentialNames().contains("cred"));
        Assert.assertArrayEquals(value, vault.getCredential("cred"));
    }

    @Test
    public void testRemoveCredential() throws VaultException, UnsupportedEncodingException {
        byte[] value = "some value".getBytes("UTF-8");
        Vault vault = new Vault(context);
        vault.storeCredential("cred", value);
        vault.removeCredential("cred");
        Assert.assertNull(vault.getCredential("cred"));
        Assert.assertFalse(vault.getCredentialNames().contains("cred"));
    }
}
