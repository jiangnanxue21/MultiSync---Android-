package com.example.multisync;

import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Map.Entry;
import java.util.TreeMap;

public class Values implements Runnable {
	private boolean btn_start_val, btn_start_enabled;
	private boolean btn_config_val, btn_config_enabled;
	private String statusStr;
	private boolean mainControl, srvControl;
	private TreeMap<String, SrvControl> srvStatus;
	
	public Values() {
		btn_start_val = true;
		btn_config_val = false;
		btn_start_enabled = true;
		btn_config_enabled = true;
		statusStr = "";
		srvStatus = new TreeMap<String, SrvControl>();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (mainControl) {
			MainActivity.btn_config.setChecked(btn_config_val);
			MainActivity.btn_config.setEnabled(btn_config_enabled);
			MainActivity.btn_start.setActivated(btn_start_val);
			MainActivity.btn_start.setEnabled(btn_start_enabled);
			MainActivity.statusText.setText(statusStr);
		}
		
		if (srvControl) {
			for (Entry<String, SrvControl> entry : srvStatus.entrySet())
				entry.getValue().redrawControls();
		}
		
		mainControl = srvControl = false;
	}
	
	public void resetAllControls() {
		MainActivity.handle.post(MainActivity.values);
	}
	
	public void forceReset() {
		mainControl = true;
		srvControl = true;
		resetAllControls();
	}
	
	public void setBtnStartVal(boolean val) {
		mainControl = true;
		btn_start_val = val;
		resetAllControls();
	}
	public void setBtnStartEnabled(boolean val) {
		mainControl = true;
		btn_start_enabled = val;
		resetAllControls();
	}
	public void setBtnConfigVal(boolean val) {
		mainControl = true;
		btn_config_val = val;
		resetAllControls();
	}
	public void setBtnConfigEnabled(boolean val) {
		mainControl = true;
		btn_config_enabled = val;
		resetAllControls();
	}
	public void setStatusStr(String val) {
		mainControl = true;
		statusStr = val;
		resetAllControls();
	}
	public TreeMap<String, SrvControl> getSrvStatus(boolean refresh) {
		srvControl = refresh;
		return srvStatus;
	}
}

class SrvControl {
	public TextView status;
	public TextView progressTxt;
	public ProgressBar progress;
	public String statusStr;
	public int progressVal;
	public String progressStr;
	
	SrvControl(TextView status, TextView progressTxt, ProgressBar progress) {
		this.status = status;
		this.progress = progress;
		this.progressTxt = progressTxt;
		
		statusStr = "Ready";
		progressVal = 0;
		progressStr = "0B/0B";
	}
	
	public void redrawControls() {
		status.setText(statusStr);
		progress.setProgress(progressVal);
		progressTxt.setText(progressStr);
	}
}