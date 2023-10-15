package com.utc.ccs.trfwsample;

import android.app.Application;

public class DKApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        DKFramework.setApplicationContext(getApplicationContext());
    }
}
