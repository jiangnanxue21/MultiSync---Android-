package com.example.multisync;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Map;
import java.util.Map.Entry;

import SyncConfig.SyncSetting;
import SyncMain.SyncProc;

public class MainActivity extends Activity {
	static SyncService syncsrv = null;
	
	public static MainActivity main_activity;
	public static TextView statusText;
	public static Switch btn_config;
	public static Button btn_start;
	
	public static Handler handle;

	private final int SPLASH_DISPLAY_LENGHT = 1000;

	public static Values values = new Values();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		statusText = (TextView) findViewById(R.id.status_text);
		btn_config = (Switch) findViewById(R.id.switch2);
		btn_start = (Button) findViewById(R.id.button);
		LinearLayout control_layout = (LinearLayout) findViewById(R.id.srv_layout);
		
		main_activity = this;
		handle = new Handler();
		
		control_layout.removeAllViews();
		for (Entry<String, SrvControl> entry : values.getSrvStatus(true).entrySet()) {
			addService(entry.getKey());
		}
		values.forceReset();


		final Runnable myRun = new Runnable() {
			@Override
			public void run() {
				btn_start.performClick();
			}
		};

		final Handler handler = new Handler() ;
		handler.postDelayed(myRun,SPLASH_DISPLAY_LENGHT);


		btn_config.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				values.setBtnConfigVal(btn_config.isChecked());
				if (btn_config.isChecked()) {
					try {
						SyncSetting.main();
						
						values.setStatusStr("please visit http://file-sync.oicp.net:9000/ to continue configure.");
						Intent intent = new Intent();        
						intent.setAction("android.intent.action.VIEW");    
						Uri content_url = Uri.parse("http://file-sync.oicp.net:9000/");
						intent.setData(content_url);  
						startActivity(intent);
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						values.setStatusStr("cannot start service: " + e.getMessage());
						values.resetAllControls();
					}
				} else {
					values.setBtnConfigEnabled(false);
					values.resetAllControls();
					SyncSetting.httpserv.stop(new Runnable() {
						@Override
						public void run() {
							values.setBtnConfigEnabled(true);
							values.setStatusStr("Server has stopped.");
							values.resetAllControls();
						}
					});
				}
			}
		});
		
		btn_start.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				values.setBtnStartVal(btn_start.isActivated());
				if (btn_start.isActivated()) {
					try {
						if (syncsrv == null) {
							syncsrv = new SyncService();
							SyncProc.loadConfig(new AndroidLogger());
							syncsrv.start();
						}
						SyncProc.pause = false;
						values.setStatusStr("SyncService started.");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						values.setStatusStr("cannot start service: " + e.getMessage());
					}
				} else {
					SyncProc.pause = true;
					values.setStatusStr("SyncService paused.");
				}
				values.resetAllControls();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public static void addService(String srvName) {
		LinearLayout control_layout = (LinearLayout) main_activity.findViewById(R.id.srv_layout);
		LinearLayout hlayout1 = new LinearLayout(main_activity);
		hlayout1.setOrientation(LinearLayout.HORIZONTAL);
		ImageView srvImage = new ImageView(main_activity);
		if (srvName.equals("baidu"))
			srvImage.setImageResource(R.drawable.baidu);
		else if (srvName.equals("dbank"))
			srvImage.setImageResource(R.drawable.dbank);
		else if (srvName.equals("dropbox"))
			srvImage.setImageResource(R.drawable.dropbox);
		else if (srvName.equals("gdrive"))
			srvImage.setImageResource(R.drawable.gdrive);
		else if (srvName.equals("kuaipan"))
			srvImage.setImageResource(R.drawable.kuaipan);
		else if (srvName.equals("skydrive"))
			srvImage.setImageResource(R.drawable.onedrive);
		else if (srvName.equals("box"))
			srvImage.setImageResource(R.drawable.box);
		else
			srvImage.setImageResource(R.drawable.ic_launcher);
		LinearLayout vlayout = new LinearLayout(main_activity);
		vlayout.setOrientation(LinearLayout.VERTICAL);
		TextView statusTxt = new TextView(main_activity);
		LinearLayout hlayout2 = new LinearLayout(main_activity);
		hlayout2.setOrientation(LinearLayout.HORIZONTAL);
		ProgressBar progress = new ProgressBar(main_activity, null, android.R.attr.progressBarStyleHorizontal);
		TextView progressTxt = new TextView(main_activity);
		
		LayoutParams tmp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		tmp.topMargin = 15;
		hlayout1.setLayoutParams(tmp);
		vlayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		hlayout2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		progress.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f));
		
		hlayout2.addView(progress);
		hlayout2.addView(progressTxt);
		vlayout.addView(statusTxt);
		vlayout.addView(hlayout2);
		hlayout1.addView(srvImage);
		hlayout1.addView(vlayout);
		control_layout.addView(hlayout1);
		
		Map<String, SrvControl> srvCons = values.getSrvStatus(true);
		if (srvCons.containsKey(srvName)) {
			SrvControl srvCon = srvCons.get(srvName);
			srvCon.progress = progress;
			srvCon.status = statusTxt;
			srvCon.progressTxt = progressTxt;
		} else
			values.getSrvStatus(true).put(srvName, new SrvControl(statusTxt, progressTxt, progress));
	}
	
	private static String simplifySize(Double size) {
		final String[] unitName = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
		int unitId = 0;
		while (size > 1024.0 && unitId < unitName.length - 1) {
			size /= 1024.0;
			++unitId;
		}
		
		return String.format("%.1f", size) + unitName[unitId];
	}
	
	public static void updateServiceStatus(String srvName, String status) {
		SrvControl srvCon = values.getSrvStatus(true).get(srvName);
		srvCon.statusStr = status;
		values.resetAllControls();
	}
	
	public static void updateFreeSpace(String srvName, long used, long total) {
		SrvControl srvCon = values.getSrvStatus(true).get(srvName);
		Double su = (double) used, st = (double) total;
		int progv = (int)((su / st) * 100);
		
		String unitU = simplifySize(su);
		String unitT = simplifySize(st);
		
		srvCon.progressStr = unitU + "/" + unitT;
		srvCon.progressVal = progv;
		values.resetAllControls();
	}
	
}

class SyncService extends Thread {
	@Override
	public void run() {
		try {
			SyncProc.main(new AndroidLogger());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
	}
}

