package com.utc.ccs.trfwsample;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.utc.fs.trframework.TRError;

public class DKAuthorizationActivity extends AppCompatActivity {
    private static String ACTIVITY_TAG = "AUTHORIZATION_ACTIVITY";

    private EditText authCodeEntry;
    private EditText dnsEntry;
    private Button loginButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        authCodeEntry = (EditText) findViewById(R.id.auth_entry);

        dnsEntry = (EditText) findViewById(R.id.dns_entry);

        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginClicked();
            }
        });

    }

    private void loginClicked() {
        disableUi();

        DKFramework.authorize(
                dnsEntry.getText().toString(),
                authCodeEntry.getText().toString(),
                new DKFramework.ErrorDelegate() {
                    @Override
                    public void onComplete(TRError trError) {
                        enableUi();

                        if (trError != null) {
                            Log.d(ACTIVITY_TAG, "Authorize Error: " + trError.getErrorMessage());

                            Toast.makeText(DKAuthorizationActivity.this, trError.getErrorMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            Intent intent = new Intent(getApplicationContext(), DKMainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    }
                });
    }


    private void disableUi() {
        progressBar.setVisibility(View.VISIBLE);
        authCodeEntry.setEnabled(false);
        dnsEntry.setEnabled(false);
        loginButton.setText(getString(R.string.logging_in));
        loginButton.setEnabled(false);
    }

    private void enableUi() {
        progressBar.setVisibility(View.GONE);
        authCodeEntry.setEnabled(true);
        dnsEntry.setEnabled(true);
        loginButton.setText(getString(R.string.login));
        loginButton.setEnabled(true);
    }
}
