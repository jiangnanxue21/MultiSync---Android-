package com.example.multisync;

import java.util.Arrays;

import SyncMain.Common;
import SyncMain.StatusLogger;

public class AndroidLogger extends StatusLogger {

	@Override
	public void show(String srvName, String msg) {
		// TODO Auto-generated method stub
		if (srvName == null || Arrays.binarySearch(Common.SERVICES, srvName) < 0) {
			System.out.println(msg);
			MainActivity.values.setStatusStr(msg);
		} else {
			MainActivity.updateServiceStatus(srvName, msg);
		}
	}

	@Override
	public void addService(String srvName) {
		// TODO Auto-generated method stub
		System.out.println("Service added: " + srvName);
		MainActivity.addService(srvName);
	}

	@Override
	public void updateSpace(String srvName, long used, long total) {
		// TODO Auto-generated method stub
		MainActivity.updateFreeSpace(srvName, used, total);
	}

}
