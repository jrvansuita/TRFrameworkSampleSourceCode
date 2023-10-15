package com.utc.ccs.trfwsample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.utc.fs.trframework.TRDevice;
import com.utc.fs.trframework.TRError;
import com.utc.fs.trframework.TRFramework;
import com.utc.fs.trframework.TRFrameworkError;

import java.util.ArrayList;


public class DKMainActivity extends AppCompatActivity {
    public static final int RESULT_LOGGED_OUT = 2;

    private static String LOG_TAG = "MAIN_ACTIVITY";

    private static int SETTINGS_REQUEST_CODE = 0;
    private ArrayList<TRDevice> nearbyDevices = new ArrayList<>();

    private ListView listView;
    private ProgressBar progressBar;
    private TextView operationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                handleRowTapped(i);
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.scan_spinner);
        progressBar.setVisibility(View.GONE);

        operationText = (TextView) findViewById(R.id.operation_txt);
        operationText.setText("");

        ImageButton settings = (ImageButton) findViewById(R.id.settings_button);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), DKSettingsActivity.class);
                startActivityForResult(intent, SETTINGS_REQUEST_CODE);
            }
        });

        if (!DKFramework.hasAuthorizedWithServer()) {
            goToAuthorizationScreen();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScanning();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_LOGGED_OUT) {
            goToAuthorizationScreen();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void goToAuthorizationScreen() {
        Intent intent = new Intent(getApplicationContext(), DKAuthorizationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleRowTapped(int row) {
        final TRDevice device = nearbyDevices.get(row);

        final Context ctx = this;

        stopScanning();

        showProgressWithText(getString(R.string.opening));

        DKFramework.openDevice(device, error -> {
            hideProgress();

            if (error == null) {
                Toast.makeText(DKMainActivity.this, "Open succes!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(DKMainActivity.this, error.getErrorMessage(), Toast.LENGTH_LONG).show();
            }

            startScanning();
        });
    }

    private void updateUi() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                getRowTitlesFromDeviceList(nearbyDevices));

        listView.setAdapter(arrayAdapter);
    }

    private void startScanning() {
        if (!DKFramework.sharedFramework().isBTLESupported()) {
            Toast.makeText(DKMainActivity.this, "BTLE is not supported on this device.", Toast.LENGTH_LONG).show();
            return;
        }

        showProgressWithText(getString(R.string.scanning));

        DKFramework.startScanning(
                () -> handleScanStarted(),
                () -> handleScanEnded(),
                error -> handleScanError(error),
                list -> handleDevicesReturned(list)
        );
    }

    private void handleScanStarted() {
        Log.d(LOG_TAG, "Scanning started");
    }

    private void handleScanEnded() {
        Log.d(LOG_TAG, "Scanning ended");
        hideProgress();
    }

    private void handleScanError(TRError error) {
        Log.d(LOG_TAG, "Scan error: " + error.getErrorMessage());
        hideProgress();

        if (error.getErrorCode() == TRFrameworkError.TRFrameworkErrorDiscoveryCancelled) {
            Log.d(LOG_TAG, "Discovery cancelled error, ignoring it");
            return;
        }

        if (error.getErrorCode() == TRFrameworkError.TRFrameworkErrorInsufficientLocationPermissions) {
            DKPermissionResolver.resolveFrameworkScanningErrors(this, error);
        }
    }

    private void handleDevicesReturned(ArrayList<TRDevice> devices) {
        nearbyDevices = devices;
        updateUi();
    }

    private void stopScanning() {
        DKFramework.stopScanning();
        hideProgress();
    }

    // Extra Helper methods
    private ArrayList<String> getRowTitlesFromDeviceList(ArrayList<TRDevice> devices) {
        ArrayList<String> serials = new ArrayList<>();

        for (TRDevice device : devices) {
            serials.add(rowTitleFromDevice(device));
        }

        return serials;
    }

    private String rowTitleFromDevice(TRDevice device) {
        String name = device.getDeviceName();

        String serial = device.getSerialNumber();

        if (name != null && !name.equals("")) {
            return name + " - " + serial;
        } else {
            return serial;
        }
    }

    private void showProgressWithText(String text) {
        progressBar.setVisibility(View.VISIBLE);
        operationText.setText(text);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        operationText.setText("");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DKPermissions.handleRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
