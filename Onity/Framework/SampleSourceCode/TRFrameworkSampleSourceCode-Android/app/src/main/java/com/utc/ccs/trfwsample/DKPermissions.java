package com.utc.ccs.trfwsample;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;

public class DKPermissions
{
    private static final String LOG_TAG = DKPermissions.class.getName();

    private static HashMap<Integer, MultiPermissionDelegate> callbacks = new HashMap<>();

    interface MultiPermissionDelegate
    {
        void onPermissionRequestComplete(HashMap<String, Boolean> results);
    }

    interface SinglePermissionDelegate
    {
        void onPermissionRequestComplete(String permission, boolean granted);
    }

    static boolean hasPermission(final Context context, final String permission)
    {
        int permissionCheck = ContextCompat.checkSelfPermission(context, permission);
        return (permissionCheck == PackageManager.PERMISSION_GRANTED);
    }

    static boolean hasAllPermissions(final Context context, final String[] permissions)
    {
        for (String permission: permissions)
        {
            if (!hasPermission(context, permission))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean hasEverRequestedPermission(final Context context, final String permission)
    {
        SharedPreferences prefs = context.getSharedPreferences(DKPermissions.class.getName(), Activity.MODE_PRIVATE);
        return prefs.getBoolean("HAS_REQUESTED_" + permission, false);
    }

    private static void setHasRequestedPermission(final Context context, final String permission)
    {
        SharedPreferences prefs = context.getSharedPreferences(DKPermissions.class.getName(), Activity.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean("HAS_REQUESTED_" + permission, true);
        prefsEditor.apply();
    }

    static boolean canRequestPermission(@NonNull final Activity activity, @NonNull final String permission)
    {
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                return !hasEverRequestedPermission(activity, permission) || activity.shouldShowRequestPermissionRationale(permission);
            }
        }
        catch (Exception ex)
        {
            Log.d(LOG_TAG, "canRequestPermission", ex);
        }

        return true;
    }

    static void requestPermissions(final Activity activity, final String permission, final int requestId, final SinglePermissionDelegate delegate)
    {
        requestPermissions(activity, new String[] { permission }, requestId, results ->
        {
            boolean granted = false;
            if (results != null)
            {
                Boolean result = results.get(permission);
                if (result != null)
                {
                    granted = result;
                }
            }

            delegate.onPermissionRequestComplete(permission, granted);
        });
    }

    static void requestPermissions(final Activity activity, final String[] permissions, final int requestId, final MultiPermissionDelegate delegate)
    {
        if (hasAllPermissions(activity, permissions))
        {
            HashMap<String,Boolean> results = new HashMap<>();
            for (String p: permissions)
            {
                results.put(p, Boolean.TRUE);
            }

            safeNotifyDelegate(delegate, results);
            removeDelegate(requestId);
        }
        else
        {
            // Wait for the results
            saveDelegate(delegate, requestId);
            ActivityCompat.requestPermissions(activity, permissions, requestId);
        }
    }

    static boolean handleRequestPermissionsResult(final Activity activity, int requestCode, String permissions[], int[] grantResults)
    {
        try
        {
            MultiPermissionDelegate delegate = null;

            if (callbacks.containsKey(requestCode))
            {
                delegate = callbacks.get(requestCode);

                if (permissions.length == grantResults.length)
                {
                    HashMap<String, Boolean> results = new HashMap<>();

                    for (int i = 0; i < permissions.length; i++)
                    {
                        String permission = permissions[i];
                        setHasRequestedPermission(activity, permission);
                        int result = grantResults[i];
                        results.put(permission, (result == PackageManager.PERMISSION_GRANTED));
                    }

                    safeNotifyDelegate(delegate, results);
                }

                removeDelegate(requestCode);
                return true;
            }
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "handleRequestPermissionsResult", ex);
        }

        return false;
    }

    private static synchronized void saveDelegate(final MultiPermissionDelegate delegate, final Integer requestId)
    {
        try
        {
            if (delegate != null)
            {
                callbacks.put(requestId, delegate);
            }
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "saveDelegate", ex);
        }
    }

    private static synchronized void removeDelegate(final Integer requestId)
    {
        try
        {
            if (callbacks != null)
            {
                callbacks.remove(requestId);
            }
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "safeNotifyDelegate", ex);
        }
    }

    private static void safeNotifyDelegate(final MultiPermissionDelegate delegate, final HashMap<String, Boolean> results)
    {
        try
        {
            if (delegate != null)
            {
                delegate.onPermissionRequestComplete(results);
            }
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "safeNotifyDelegate", ex);
        }
    }
}