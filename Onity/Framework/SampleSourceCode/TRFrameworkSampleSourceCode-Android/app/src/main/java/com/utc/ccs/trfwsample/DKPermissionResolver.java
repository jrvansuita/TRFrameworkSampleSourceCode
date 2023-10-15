package com.utc.ccs.trfwsample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import com.utc.fs.trframework.TRError;

import androidx.annotation.NonNull;

public class DKPermissionResolver
{
    static void resolveFrameworkScanningErrors(@NonNull final Activity activity, @NonNull final TRError error)
    {
        showRequestLocationPermissionsDialog(activity, error.getRequiredPermissionsExtra());
    }

    private static void showRequestLocationPermissionsDialog(@NonNull final Activity activity, @NonNull final String[] permissionsToRequest)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Unable to scan for devices");

        StringBuilder msg = new StringBuilder();
        msg.append("TRFramework requires the following location permissions:\n");

        for (String s : permissionsToRequest)
        {
            msg.append("\n");
            msg.append(s);
        }

        boolean canRequestProgramatically = true;

        for (String s: permissionsToRequest)
        {
            if (!DKPermissions.canRequestPermission(activity, s))
            {
                canRequestProgramatically = false;
                break;
            }
        }

        if (canRequestProgramatically)
        {
            msg.append("\n\nTap request permissions and then Allow on the following dialog.");
        }
        else
        {
            msg.append("\n\nTap app settings and then enable location permissions for the app.");
        }

        builder.setMessage(msg.toString());

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });

        final boolean canRequest = canRequestProgramatically;
        builder.setNeutralButton(canRequestProgramatically ? "Request Permission" : "App Settings", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();

                if (canRequest)
                {
                    DKPermissions.requestPermissions(activity, permissionsToRequest, 9876, results ->
                    {

                    });
                }
                else
                {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}



