package ch.suzukieng.andvault;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link VaultStorage} implementation that uses {@link SharedPreferences} for storage (= file in
 * a local filesystem).
 *
 * @author Alex Suzuki, Suzuki Engineering GmbH, 2015
 */
public class SharedPrefsStorage implements VaultStorage {

    private static final String PREFS_NAME = "vault";

    private static final String PREF_NAME_KEY = "key";

    private static final String PREF_PREFIX_CREDENTIAL = "credential_";

    private Context context;

    public SharedPrefsStorage(Context context) {
        this.context = context;
    }

    private SharedPreferences getSharedPrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void setCredential(String name, byte[] value) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Credential name must not be null or empty");
        }
        final String encodedCredential = Base64.encodeToString(value, Base64.NO_WRAP);
        getSharedPrefs().edit().putString(PREF_PREFIX_CREDENTIAL + name, encodedCredential).commit();
    }

    @Override
    public byte[] getCredential(String name) {
        final String encodedCred = getSharedPrefs().getString(PREF_PREFIX_CREDENTIAL + name, null);
        if (encodedCred == null) {
            return null;
        }

        return Base64.decode(encodedCred, Base64.NO_WRAP);
    }

    @Override
    public List<String> getCredentialNames() {
        final Map<String, ?> prefsMap = getSharedPrefs().getAll();
        List<String> credentialNames = new ArrayList<String>(prefsMap.size());
        for (String prefKey : prefsMap.keySet()) {
            if (prefKey.startsWith(PREF_PREFIX_CREDENTIAL)) {
                credentialNames.add(prefKey.substring(PREF_PREFIX_CREDENTIAL.length()));
            }
        }
        return credentialNames;
    }

    @Override
    public void removeCredential(String name) {
        final SharedPreferences prefs = getSharedPrefs();
        if (prefs.contains(PREF_PREFIX_CREDENTIAL + name)) {
            prefs.edit().remove(PREF_PREFIX_CREDENTIAL + name).commit();
        }
    }

    @Override
    public void setKey(byte[] key) {
        getSharedPrefs().edit().putString(PREF_NAME_KEY, Base64.encodeToString(key, Base64.NO_WRAP)).commit();
    }

    @Override
    public byte[] getKey() {
        final String vaultKey = getSharedPrefs().getString(PREF_NAME_KEY, null);
        if (vaultKey == null) {
            return null;
        } else {
            return Base64.decode(vaultKey, Base64.NO_WRAP);
        }
    }

    @Override
    public void reset() {
        getSharedPrefs().edit().clear().commit();
    }

}
