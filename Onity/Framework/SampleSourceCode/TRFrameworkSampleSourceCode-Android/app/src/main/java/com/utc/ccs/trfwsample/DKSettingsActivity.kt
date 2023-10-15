package com.utc.ccs.trfwsample;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.utc.fs.trframework.TRSyncType;

public class DKSettingsActivity extends AppCompatActivity {
    private Button syncButton;
    private ProgressBar syncSpinner;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupUi();

    }

    private void setupUi() {
        TextView authCode = (TextView) findViewById(R.id.key_serial_txt);
        authCode.setText(getResources().getString(R.string.key_serial_format, DKFramework.sharedFramework().getLocalDeviceSerialNumber()));

        TextView dns = (TextView) findViewById(R.id.sync_dns_txt);
        dns.setText(getResources().getString(R.string.sync_dns_format, DKFramework.sharedFramework().getSyncUrl()));


        syncSpinner = (ProgressBar) findViewById(R.id.sync_spinner);
        syncSpinner.setVisibility(View.GONE);

        syncButton = (Button) findViewById(R.id.sync_button);
        syncButton.setOnClickListener(view -> syncClicked());

        logoutButton = (Button) findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(view -> logoutClicked());
    }


    private void logoutClicked() {
        DKFramework.resetFramework();

        setResult(DKMainActivity.RESULT_LOGGED_OUT);
        finish();
    }

    private void syncClicked() {
        syncButton.setText(getString(R.string.syncing));
        lockButtons();

        DKFramework.updateKey(true, TRSyncType.TRSyncTypeFull, (didPerformSync, error) -> {
            syncButton.setText(getString(R.string.sync));
            unlockButtons();

            if (error != null) {
                Toast.makeText(DKSettingsActivity.this, error.getErrorMessage(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(DKSettingsActivity.this, "Sync was successful", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void lockButtons() {
        syncButton.setEnabled(false);
        logoutButton.setEnabled(false);

        syncButton.setVisibility(View.INVISIBLE);
        syncSpinner.setVisibility(View.VISIBLE);
    }

    private void unlockButtons() {
        syncButton.setEnabled(true);
        logoutButton.setEnabled(true);

        syncButton.setVisibility(View.VISIBLE);
        syncSpinner.setVisibility(View.GONE);
    }
}
