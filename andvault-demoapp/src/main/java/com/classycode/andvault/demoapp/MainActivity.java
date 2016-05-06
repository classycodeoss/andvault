package com.classycode.andvault.demoapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.classycode.andvault.Vault;
import com.classycode.andvault.VaultException;

public class MainActivity extends AppCompatActivity implements CredentialListFragment.CredentialListFragmentListener, CredentialFragment.CredentialFragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Vault vault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new CredentialListFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Vault.isDeviceProtected(getApplicationContext())) {
            try {
                vault = new Vault(getApplicationContext());
            } catch (VaultException e) {
                Log.e(TAG, "Initialization of Vault failed", e);
                showErrorDialog(getString(R.string.error_vault_initialization_failed_fmt, e.getMessage()));
                Vault.reset(getApplicationContext());
                try {
                    vault = new Vault(getApplicationContext());
                } catch (VaultException e1) {
                    throw new IllegalStateException("Vault re-initialization failed", e1);
                }
            }
        } else {
            showErrorDialog(getString(R.string.error_device_not_protected));
            vault = null;
        }
    }

    Vault getVault() {
        return vault;
    }

    @Override
    public void onCredentialSelected(String name) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                CredentialFragment.newInstance(name)).addToBackStack("CredentialFragment").commit();
    }

    @Override
    public void onDeleteCredential(String name) {
        getVault().removeCredential(name);
        getSupportFragmentManager().popBackStack();
    }

    void showErrorDialog(String msg) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
