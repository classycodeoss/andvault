package ch.suzukieng.droidvault.demoapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import ch.suzukieng.droidvault.Vault;

public class MainActivity extends AppCompatActivity implements CredentialListFragment.CredentialListFragmentListener, CredentialFragment.CredentialFragmentListener {

    private Vault vault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new CredentialListFragment()).commit();
        vault = new Vault(getApplicationContext());
    }

    public Vault getVault() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }
}
