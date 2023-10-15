package com.utc.ccs.trfwsample;

/***
 *
 * DKFrameworkConfig
 * TRFramework - Sample
 *
 * Copyright © 2017 United Technologies Corporation. All rights reserved.
 *
 * DKFrameworkConfig is a common place to define constants used by callers
 * of TRFramework.
 *
 */
public class DKFrameworkConfig
{
    //
    // In the best effort approach tok keeping the key up to date always, it is a
    // good idea to enforce a periodic credential sync.  We use 24 hours in the
    // sample, but this value is really dependant upon the system TRFramework
    // is being used in.
    //
    public static final long credentialSyncFrequency  = 60 * 60 * 24 * 1000;

    //
    // In a hotel based app, using quick auth on the users guest room will
    // help speed up the time the user has to wait in the app.  A simple approach
    // to filter devices to quick connect to is to use access category.
    //
    public static final String quickConnectAccessCategory = "guest";

    //
    // In order to reduce BTLE usage on the phone, if a scan goes for longer than
    // a specified period of time, quick connect will not be used.
    //
    public static final long quickConnectTimeout = 60 * 1000;

    //
    // Only use quick connect when the phone has a relatively strong signal to
    // the broker.
    //
    public static final int quickConnectRssiThreshold = -100;
}
