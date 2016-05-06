package com.classycode.andvault.demoapp;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import com.classycode.andvault.Vault;
import com.classycode.andvault.VaultException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2015
 */
public class CredentialListFragment extends ListFragment {

    public interface CredentialListFragmentListener {

        void onCredentialSelected(String name);
    }

    private CredentialListFragmentListener listener;

    private TextView deviceProtectionLabel;

    private TextView secureElementLabel;

    private class CredentialListAdapter extends ArrayAdapter<String> {

        public CredentialListAdapter(Context context, List<String> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.credential_list_item, parent, false);
            }
            final String credentialName = getItem(position);
            ((TextView) view.findViewById(R.id.credential_name_label)).setText(credentialName);
            return view;
        }

    }

    private Vault getVault() {
        return ((MainActivity) getActivity()).getVault();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.credential_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        deviceProtectionLabel = (TextView) view.findViewById(R.id.device_protection_info_label);
        secureElementLabel = (TextView) view.findViewById(R.id.secure_element_info_label);

    }

    @Override
    public void onResume() {
        super.onResume();

        if (Vault.isDeviceProtected(getContext())) {
            deviceProtectionLabel.setText(R.string.device_protection_enabled);
            deviceProtectionLabel.setBackgroundColor(getResources().getColor(R.color.positive));
        } else {
            deviceProtectionLabel.setText(R.string.device_protection_disabled);
            deviceProtectionLabel.setBackgroundColor(getResources().getColor(R.color.negative));
        }

        if (Vault.isHardwareBackedCredentialStorage()) {
            secureElementLabel.setText(R.string.secure_element_enabled);
            secureElementLabel.setBackgroundColor(getResources().getColor(R.color.positive));
        } else {
            secureElementLabel.setText(R.string.secure_element_disabled);
            secureElementLabel.setBackgroundColor(getResources().getColor(R.color.negative));
        }

        refreshCredentials();

        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (CredentialListFragmentListener) context;
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        if (getVault() != null) {
            inflater.inflate(R.menu.credential_list_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_credential_item) {
            showAddCredentialDialog();
            return true;
        } else if (item.getItemId() == R.id.reset_vault_item) {
            Vault.reset(getContext());
            refreshCredentials();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showAddCredentialDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.add_credential_title);

        final View layout = LayoutInflater.from(getContext()).inflate(R.layout.credential_entry_dialog, null);

        final EditText nameField = (EditText) layout.findViewById(R.id.credential_name_field);
        final EditText valueField = (EditText) layout.findViewById(R.id.credential_value_field);

        final EditText input = new EditText(getContext());
        input.setSingleLine();
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        builder.setView(layout, getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                getResources().getDimensionPixelSize(R.dimen.content_margin),
                getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                getResources().getDimensionPixelSize(R.dimen.content_margin));
        builder.setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String name = nameField.getText().toString().trim();
                nameField.setText("");
                if (!name.isEmpty()) {
                    try {
                        final String value = valueField.getText().toString();
                        valueField.setText("");
                        getVault().storeStringCredential(name, value);
                    } catch (VaultException e) {
                        ((MainActivity) getActivity()).showErrorDialog(e.getMessage());
                    }
                    refreshCredentials();
                    dialog.dismiss();
                }
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String credentialName = (String) getListAdapter().getItem(position);
        listener.onCredentialSelected(credentialName);
    }

    private void refreshCredentials() {
        if (getVault() != null) {
            setListAdapter(new CredentialListAdapter(getContext(), getVault().getCredentialNames()));
        }
        else {
            setListAdapter(null);
        }
    }
}
