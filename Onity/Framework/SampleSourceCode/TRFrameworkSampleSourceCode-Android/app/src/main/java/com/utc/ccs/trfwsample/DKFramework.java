package com.utc.ccs.trfwsample;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utc.fs.trframework.TRDevice;
import com.utc.fs.trframework.TRDiscoveryRequest;
import com.utc.fs.trframework.TRError;
import com.utc.fs.trframework.TRFramework;
import com.utc.fs.trframework.TRSyncRequest;
import com.utc.fs.trframework.TRSyncType;

import java.util.ArrayList;
import java.util.UUID;

import static com.utc.fs.trframework.TRSyncType.TRSyncTypeActivity;
import static com.utc.fs.trframework.TRSyncType.TRSyncTypeFull;

/***
 *
 * DKFramework
 * TRFramework - Sample
 *
 * Copyright © 2017 United Technologies Corporation. All rights reserved.
 *
 * DKFramework is an application level wrapper around TRFramework that simplifies
 * some of the common usage patterns of TRFramework.  The sample is geared
 * towards the hospitality industry but is applicable to all cases of framework
 * usage.
 *
 * This class can be copied into your project as a starter or used as a reference
 * for your implementation of a TRFramework based app.
 *
 */
public class DKFramework
{
    private static final String LOG_TAG = "DKFramework";
    private static final String frameworkPasswordKey = "FrameworkPassword";
    private static final String lastCredentialSyncKey = "LastCredentialSync";

    private static Context applicationContext;

    public static void setApplicationContext(@NonNull final Context context)
    {
        applicationContext = context.getApplicationContext();
    }

    private static void checkContext()
    {
        if (applicationContext == null)
        {
            throw new RuntimeException("applicationContext is null.  It is not valid to use DKFramework until setApplicationContext has been called.");
        }
    }

    public interface VoidDelegate
    {
        void onComplete();
    }

    public interface BoolDelegate
    {
        void onComplete(boolean result);
    }

    public interface SyncCompleteDelegate
    {
        void onComplete(boolean didPerformSync, @Nullable TRError error);
    }

    public interface DeviceListDelegate
    {
        void onComplete(@NonNull ArrayList<TRDevice> list);
    }

    public interface ErrorDelegate
    {
        void onComplete(@Nullable TRError error);
    }

    //
    // Wrapper to return a valid instance of TRFramework for easier application
    // use.  This method handles initializing the framework if needed.
    //
    public static TRFramework sharedFramework()
    {
        checkContext();

        TRFramework fw = TRFramework.sharedInstance();
        if (fw == null)
        {
            initFramework();
            fw = TRFramework.sharedInstance();
        }

        if (fw == null)
        {
            // This is an error case that should never happen, but if it does,
            // the only way to receover is to force quit the app and try again.
            // If that does not work, the app will need to be deleted and
            // re-installed.
            Log.e(LOG_TAG, "Unable to initialize TRFramework.  Delete and re-install application");
            System.exit(-1);
        }

        return fw;
    }

    //
    // Use an app generated GUID for the framework password.  The password is
    // stored in secure prefs.
    //
    private static String frameworkPassword()
    {
        checkContext();

        String pwd = null;

        try
        {
            pwd = sharedPreferences().getString(frameworkPasswordKey, null);
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "Error fetching framework password. We must reset the framework and start over.", ex);
            TRError resetErr = TRFramework.resetSharedFramework(applicationContext);
            Log.d(LOG_TAG, "resetSharedFramework returned " + errToString(resetErr));
        }

        if (pwd == null)
        {
            pwd = UUID.randomUUID().toString();

            SharedPreferences.Editor editor = getEditor();
            editor.putString(frameworkPasswordKey, pwd);
            editor.apply();

            Log.d(LOG_TAG, "Saved framework passwod to prefs");
        }

        return pwd;
    }

    //
    // The DirectKey system provides an additional security mechanism of passing a
    // pin code to authenticate the user for any open operation.  In app where
    // the user is authorized in some other manner, apps can securely retreive
    // the user pin code from the Core API and let use it without user
    // interaction, or simply hardcode a pin code on the mobile side, as this
    // sample has done.
    //
    private static String userPin()
    {
        return "1234";
    }

    //
    // Initialize the framework with a stored application password.  If the stored
    // password fails, reset and re-initialize the framework.
    //
    private static void initFramework()
    {
        checkContext();

        TRError err = TRFramework.initFramework(applicationContext, frameworkPassword());
        Log.d(LOG_TAG, "initFramework returned " + errToString(err));
        if (err != null)
        {
            err = TRFramework.resetSharedFramework(applicationContext);
            Log.d(LOG_TAG, "resetSharedFramework returned " + errToString(err));
            if (err == null)
            {
                err = TRFramework.initFramework(applicationContext, frameworkPassword());
                Log.d(LOG_TAG, "initFramework returned " + errToString(err));
            }
        }
    }

    //
    // Reset the framework
    //
    public static void resetFramework()
    {
        checkContext();

        TRError err = TRFramework.resetSharedFramework(applicationContext);
        Log.d(LOG_TAG, "resetSharedFramework returned " + errToString(err));
    }

    //
    // Authorize the framework and perform a full sync
    //
    public static void authorize(
            @NonNull final String host,
            @NonNull final String authCode,
            @NonNull final ErrorDelegate delegate)
    {
        resetFramework();

        sharedFramework().setAuthorizationCode(authCode, host, userPin(), new TRFramework.TRErrorDelegate()
        {
            @Override
            public void onComplete(TRError authorizeError)
            {
                Log.d(LOG_TAG, "Auth complete, error " + errToString(authorizeError));

                if (authorizeError == null)
                {
                    updateKey(true, TRSyncType.TRSyncTypeFull, new SyncCompleteDelegate()
                    {
                        @Override
                        public void onComplete(boolean didPerformSync, TRError syncError)
                        {
                            Log.d(LOG_TAG, "Sync after auth complete, error: " + errToString(syncError));
                            safeNotify(delegate, syncError);
                        }
                    });
                }
                else
                {
                    safeNotify(delegate, authorizeError);
                }
            }
        });
    }

    //
    // Helper method that checks to see if the framework has been authorized
    //
    public static boolean hasAuthorizedWithServer()
    {
        String serialNumber = sharedFramework().getLocalDeviceSerialNumber();
        return (serialNumber != null && serialNumber.length() > 0);
    }

    //
    // Helper method to determine if its appropriate to scan for devices.  If
    // the framework is not authorized or if there are no permissions downloaded
    // it doesn't make sense to use the BTLE radio because the framework cannot
    // interact with any devices.
    //
    private static void canScanForDevices(@NonNull final BoolDelegate delegate)
    {
        if (!hasAuthorizedWithServer())
        {
            safeNotify(delegate, false);
        }
        else
        {
            sharedFramework().countAuthorizedDevices(new TRFramework.TRLongDelegate()
            {
                @Override
                public void onComplete(Long aLong, TRError trError)
                {
                    safeNotify(delegate, aLong != null && aLong > 0);
                }
            });
        }
    }

    private static long lastCredentialSyncTime()
    {
        return sharedPreferences().getLong(lastCredentialSyncKey, 0L);
    }

    private static void setLastCredentialSyncTime(final long date)
    {
        SharedPreferences.Editor editor = getEditor();
        editor.putLong(lastCredentialSyncKey, date);
        editor.apply();
    }

    private static void shouldUpdateKey(
            boolean force,
            @NonNull final BoolDelegate delegate)
    {
        if (!hasAuthorizedWithServer())
        {
            Log.d(LOG_TAG, "Framework is not authorized, shouldUpdateKey: false");
            safeNotify(delegate, false);
            return;
        }

        if (sharedFramework().getActiveSyncRequest() != null)
        {
            Log.d(LOG_TAG, "Framework is already performing a sync, shouldUpdateKey: false");
            safeNotify(delegate, false);
            return;
        }

        if (force)
        {
            Log.d(LOG_TAG, "Forcing update, shouldUpdateKey: true");
            safeNotify(delegate, true);
            return;
        }

        TRFramework fw = sharedFramework();
        if (!fw.isLocalDeviceUpToDate())
        {
            Log.d(LOG_TAG, "Framework credentials are expired, shouldUpdateKey: true");
            safeNotify(delegate, true);
            return;
        }

        long timeSinceLastCredentialSync = System.currentTimeMillis() - lastCredentialSyncTime();
        if (timeSinceLastCredentialSync > DKFrameworkConfig.credentialSyncFrequency)
        {
            Log.d(LOG_TAG, "Last credential sync threshold, shouldUpdateKey: true");
            safeNotify(delegate, true);
            return;
        }

        sharedFramework().countAuthorizedDevices(new TRFramework.TRLongDelegate()
        {
            @Override
            public void onComplete(Long aLong, TRError trError)
            {
                if (aLong == null || aLong == 0)
                {
                    Log.d(LOG_TAG, "No permissions have been downloaded, shouldUpdateKey: true");
                    safeNotify(delegate, true);
                }
                else
                {
                    Log.d(LOG_TAG, "Nothing to update, shouldUpdateKey: false");
                    safeNotify(delegate, false);
                }
            }
        });
    }

    //
    // After request open operations an Activity sync is performed by default,
    // but this method checks the last credential sync time and if the sync
    // frequency has passed, use this opportunity to perform a full credential
    // sync.
    //
    private static TRSyncType adjustSyncType(final TRSyncType syncType)
    {
        TRSyncType adjustedSyncType = syncType;

        if (syncType == TRSyncTypeActivity)
        {
            long timeSinceLastCredentialSync = System.currentTimeMillis() - lastCredentialSyncTime();
            if (timeSinceLastCredentialSync > DKFrameworkConfig.credentialSyncFrequency)
            {
                Log.d(LOG_TAG, "Adjusting sync type from Activity to full");
                adjustedSyncType = TRSyncTypeFull;
            }
        }

        return adjustedSyncType;
    }

    public static void updateKey()
    {
        updateKey(false, TRSyncTypeFull, null);
    }

    public static void updateKey(
            final boolean force,
            final TRSyncType syncType,
            @Nullable final SyncCompleteDelegate delegate)
    {
        shouldUpdateKey(force, new BoolDelegate()
        {
            @Override
            public void onComplete(boolean shouldSync)
            {
                if (shouldSync)
                {
                    TRSyncType adjustedSyncType = adjustSyncType(syncType);
                    Log.d(LOG_TAG, "Adjusted Sync Request type: " + adjustedSyncType);

                    final TRSyncRequest syncRequest = TRSyncRequest.requestWithSyncType(adjustedSyncType);
                    syncRequest.setSyncCompletedCallback(new TRFramework.TRErrorDelegate()
                    {
                        @Override
                        public void onComplete(TRError syncError)
                        {
                            Log.d(LOG_TAG, "Sync Completed, error: " + errToString(syncError));

                            if (didSyncCredentials(syncRequest))
                            {
                                setLastCredentialSyncTime(System.currentTimeMillis());
                            }

                            safeNotify(delegate, true, syncError);
                        }
                    });

                    sharedFramework().requestSyncWithServer(syncRequest);
                }
                else
                {
                    safeNotify(delegate, false, null);
                }
            }
        });
    }

    private static boolean didSyncCredentials(@Nullable final TRSyncRequest request)
    {
        if (request != null)
        {
            switch (request.getSyncType())
            {
                case TRSyncTypeCredentials:
                case TRSyncTypeFull:
                {
                    return true;
                }

                default:
                {
                    return false;
                }
            }
        }

        return false;
    }

    public static void startScanning(
            @Nullable final VoidDelegate scanStartedDelegate,
            @Nullable final VoidDelegate scanEndedDelegate,
            @Nullable final ErrorDelegate scanErrorDelegate,
            @Nullable final DeviceListDelegate doorsChangedDelegate)
    {
        final TRDiscoveryRequest discoveryRequest = TRDiscoveryRequest.discoveryRequest();
        discoveryRequest.setDelegate(new TRDiscoveryRequest.TRDiscoveryDelegate()
        {
            @Override
            public void discoveryStarted()
            {
                safeNotify(scanStartedDelegate);
            }

            @Override
            public void discoveryEnded()
            {
                safeNotify(scanEndedDelegate);
            }

            @Override
            public void nearbyDevicesChanged(@NonNull ArrayList<TRDevice> arrayList)
            {
                safeNotify(doorsChangedDelegate, arrayList);
            }

            @Override
            public void discoveryError(boolean scanHalted, @Nullable TRError discoveryError)
            {
                safeNotify(scanErrorDelegate, discoveryError);
            }
        });

        discoveryRequest.setDeviceShouldAuthenticateDelegate(new TRDiscoveryRequest.TRShouldAuthenticateDelegate()
        {
            @Override
            public boolean shouldAuthenticateDevice(@NonNull TRDiscoveryRequest dr, @NonNull TRDevice trDevice)
            {
                return shouldQuickAuthToDevice(dr, trDevice);
            }
        });

        discoveryRequest.setPinCodeForAuthentication(userPin());

        canScanForDevices(new BoolDelegate()
        {
            @Override
            public void onComplete(boolean result)
            {
                if (result)
                {
                    sharedFramework().stopDiscovery();
                    sharedFramework().startDiscovery(discoveryRequest);
                }
                else
                {
                    Log.d(LOG_TAG, "TRFramework is not authorized or has no credentials, cannot start scan now");
                    safeNotify(doorsChangedDelegate, new ArrayList<TRDevice>());
                }
            }
        });
    }

    public static void openDevice(@NonNull final TRDevice device, @NonNull final ErrorDelegate delegate)
    {
        DKFramework.sharedFramework().requestOpen(device, userPin(), new TRFramework.TRDeviceActionDelegate()
        {
            @Override
            public void onComplete(TRDevice trDevice, boolean success, TRError err)
            {
                Log.d(LOG_TAG, "Open door finished, success: " + success + ", error: " + errToString(err));

                if (success)
                {
                    updateKey(true, TRSyncTypeActivity, null);
                }

                safeNotify(delegate, err);
            }
        });
    }

    public static void stopScanning()
    {
        sharedFramework().stopDiscovery();
    }

    // Example criteria for quick connect:
    //
    // 1. Scan has not been going for more than 60 seconds.
    // 2. Privacy Dead Bolt NOT set - The assumption here is that a hotel
    //    guest is most likely inside their room if the deadbolt is engaged,
    //    so the app doesn't need to attempt a connection to the broker.
    // 3. Access category is 'guest'. The assumption here is that
    //    this is something to indicate this is a user guest room vs. a
    //    public or shared door.
    // 4. RSSI above a certain threshold.  The idea here is that the app would
    //    only trigger the authentication step once the user is relatively
    //    close to the door.
    //
    private static boolean shouldQuickAuthToDevice(
            @NonNull final TRDiscoveryRequest discoveryRequest,
            @NonNull final TRDevice device)
    {
        long discoveryStart = discoveryRequest.getDiscoveryBeginTime();
        long now = System.currentTimeMillis();
        long discoveryDuration = now - discoveryStart;
        if (discoveryDuration > DKFrameworkConfig.quickConnectTimeout)
        {
            Log.d(LOG_TAG, "Scan has been ongoing for longer than 60 seconds, do not use quick auth");
            return false;
        }

        if (device.isPrivacyDeadboltSet())
        {
            Log.d(LOG_TAG, "Privacy deadbolt is set, do not use quick auth");
            return false;
        }

        if (device.getAccessCategory() != null && !device.getAccessCategory().equals(DKFrameworkConfig.quickConnectAccessCategory))
        {
            Log.d(LOG_TAG, "Device does not have correct access category, do not use quick auth");
            return false;
        }

        Integer rssi = device.getAverageRssiValue();
        if (rssi == null)
        {
            Log.d(LOG_TAG, "No signal strength for device, do not use quick auth");
            return false;
        }

        if (rssi < DKFrameworkConfig.quickConnectRssiThreshold)
        {
            Log.d(LOG_TAG, "Signal strength too low, do not use quick auth");
            return false;
        }

        Log.d(LOG_TAG, "All checks pass, returning true to allow quick auth");
        return true;
    }

    private static SharedPreferences sharedPreferences()
    {
        return applicationContext.getSharedPreferences("DKFramework", Activity.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getEditor()
    {
        return sharedPreferences().edit();
    }

    private static String errToString(@Nullable final TRError err)
    {
        if (err == null)
        {
            return "null";
        }
        else
        {
            return err.toString();
        }
    }

    private static void safeNotify(final BoolDelegate delegate, final boolean result)
    {
        try
        {
            if (delegate != null)
            {
                delegate.onComplete(result);
            }
        }
        catch (Exception ex)
        {
            Log.d(LOG_TAG, "safeNotify.BoolDelegate", ex);
        }
    }

    private static void safeNotify(final SyncCompleteDelegate delegate, final boolean didPerformSync, final TRError error)
    {
        try
        {
            if (delegate != null)
            {
                delegate.onComplete(didPerformSync, error);
            }
        }
        catch (Exception ex)
        {
            Log.d(LOG_TAG, "safeNotify.SyncCompleteDelegate", ex);
        }
    }

    private static void safeNotify(final ErrorDelegate delegate, final TRError error)
    {
        try
        {
            if (delegate != null)
            {
                delegate.onComplete(error);
            }
        }
        catch (Exception ex)
        {
            Log.d(LOG_TAG, "safeNotify.ErrorDelegate", ex);
        }
    }

    private static void safeNotify(final VoidDelegate delegate)
    {
        try
        {
            if (delegate != null)
            {
                delegate.onComplete();
            }
        }
        catch (Exception ex)
        {
            Log.d(LOG_TAG, "safeNotify.VoidDelegate", ex);
        }
    }

    private static void safeNotify(final DeviceListDelegate delegate, @NonNull ArrayList<TRDevice> list)
    {
        try
        {
            if (delegate != null)
            {
                delegate.onComplete(list);
            }
        }
        catch (Exception ex)
        {
            Log.d(LOG_TAG, "safeNotify.DeviceListDelegate", ex);
        }
    }
}
