package com.cpixelarts.pixelarts;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by vincent on 07/11/14.
 */
public class CPixelArtsApplication extends Application {
    private final String PROPERTY_ID = "UA-44614143-5";

    Tracker mTracker = null;

    synchronized Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(PROPERTY_ID);
            mTracker.enableAdvertisingIdCollection(true);
            mTracker.enableExceptionReporting(true);
        }

        return mTracker;
    }
}
