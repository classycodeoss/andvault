package ch.suzukieng.droidvault.demoapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import ch.suzukieng.droidvault.VaultException;

/**
 * @author Alex Suzuki, Suzuki Engineering GmbH, 2015
 */
public class CredentialFragment extends Fragment {

    public interface CredentialFragmentListener {

        void onDeleteCredential(String name);
    }

    private static final String ARG_CREDENTIAL_NAME = "credentialName";

    private TextView credentialValueLabel;

    private CredentialFragmentListener listener;

    static CredentialFragment newInstance(String credentialName) {
        CredentialFragment fragment = new CredentialFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CREDENTIAL_NAME, credentialName);
        fragment.setArguments(args);
        return fragment;
    }

    private String getCredentialName() {
        return getArguments().getString(ARG_CREDENTIAL_NAME);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.credential_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView credentialNameLabel = (TextView) view.findViewById(R.id.credential_name_label);
        credentialNameLabel.setText(getCredentialName());

        credentialValueLabel = (TextView) view.findViewById(R.id.credential_value_label);
        credentialValueLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String value = ((MainActivity) getActivity()).getVault().getStringCredential(getCredentialName());
                    credentialValueLabel.setText(value);

                    // clear label after a couple of seconds
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isResumed()) {
                                credentialValueLabel.setText(R.string.value_hidden);
                            }
                        }
                    }, 2500);
                } catch (VaultException e) {
                    ((MainActivity) getActivity()).showErrorDialog(e.getMessage());
                }
            }
        });

        Button deleteCredentialButton = (Button) view.findViewById(R.id.delete_credential_button);
        deleteCredentialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteCredential(getCredentialName());
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (CredentialFragmentListener) context;
    }

    @Override
    public void onResume() {
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        super.onResume();
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().getSupportFragmentManager().popBackStack();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
