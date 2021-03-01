package com.ryd.datastore;


import android.app.Application;

import androidx.multidex.MultiDex;

public class DataStoreApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
    }
}
