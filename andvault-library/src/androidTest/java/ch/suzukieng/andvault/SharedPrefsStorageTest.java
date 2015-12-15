package ch.suzukieng.andvault;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
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
public class SharedPrefsStorageTest {

    private SharedPrefsStorage storage;

    private Context context;

    @Before
    public void setupStorage() {
        context = new RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "SharedPrefsStorageTest");
        storage = new SharedPrefsStorage(context);
    }

    @Test
    public void testStorageIsInitiallyEmpty() {
        Assert.assertEquals(0, storage.getCredentialNames().size());
        Assert.assertNull(storage.getKey());
    }

    @Test
    public void testStoreLoadKey() throws UnsupportedEncodingException {
        byte[] key = "this is a key".getBytes("UTF-8");
        storage.setKey(key);
        Assert.assertArrayEquals(key, storage.getKey());
    }

    @Test
    public void testStoreCredential() throws UnsupportedEncodingException {
        byte[] value = "this is an encrypted value".getBytes("UTF-8");
        storage.setCredential("some name", value);
        Assert.assertTrue(storage.getCredentialNames().contains("some name"));
        Assert.assertArrayEquals(value, storage.getCredential("some name"));
    }

    @Test
    public void testRemoveCredential() throws UnsupportedEncodingException {
        byte[] value = "this is an encrypted value".getBytes("UTF-8");
        storage.setCredential("some name", value);
        Assert.assertTrue(storage.getCredentialNames().contains("some name"));
        storage.removeCredential("some name");
        Assert.assertFalse(storage.getCredentialNames().contains("some name"));
        Assert.assertNull(storage.getCredential("some name"));
    }


    @Test
    public void testReset() throws UnsupportedEncodingException {
        byte[] value = "this is an encrypted value".getBytes("UTF-8");
        storage.setCredential("some name", value);
        storage.reset();
        Assert.assertEquals(0, storage.getCredentialNames().size());
        Assert.assertNull(storage.getKey());
    }
}
