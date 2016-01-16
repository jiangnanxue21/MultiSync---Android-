package com.example.multisync;

import SyncMain.Common;
import android.app.Application;
import android.os.Environment;

public class ContextUtil extends Application {
	private static ContextUtil instance;
	
	public static ContextUtil getInstance() {
        return instance;
    }
	
	@Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        instance = this;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        	Common.Init(Environment.getExternalStorageDirectory() + "/.multisync/");
		} else {
			Common.Init(Environment.getDataDirectory() + "/.multisync/");
		}
        
    }
}
