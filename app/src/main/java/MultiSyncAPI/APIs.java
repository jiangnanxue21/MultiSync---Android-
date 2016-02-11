package MultiSyncAPI;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import SyncMain.Common;
import SyncMain.SyncProc;

public abstract class APIs {
	public String api_name;
	public long free_space;
	public TreeSet<String> root_list;
	public MultiProcess thread;
	public long probeTime ;
	
	public APIs() {
		root_list = new TreeSet<String>();
	}
	
	public abstract void init(String sync_path, String remote_path, String token1, String token2) throws Exception;
	public abstract void getFreeSpace() throws Exception;
	public abstract void downloadFile(String filename, String remotename) throws Exception;
	public abstract void uploadFile(String filename, String remotename) throws Exception;
	public abstract void createDir(String dirname) throws Exception;
	public abstract void deleteFile(String filename) throws Exception;
	public abstract String getFileContent(String filename) throws Exception;
	public abstract void refreshList() throws Exception;
	
	public void callAPI(Map<String, String> params) throws Exception {
		String func = params.get("func").toString();
		if (func.equals("getFreeSpace")) {
			SyncProc.log.show(api_name, "Get avaliable size...");
			getFreeSpace();
			SyncProc.log.show(api_name, "Ready");
		} else if (func.equals("downloadFile")) {
			SyncProc.log.show(api_name, "Downloading...");
			downloadFile(params.get("filename"), params.get("remotename"));
			SyncProc.log.show(api_name, "Ready");
		} else if (func.equals("uploadFile")) {
			SyncProc.log.show(api_name, "uploading...");
			uploadFile(params.get("filename"), params.get("remotename"));
			SyncProc.log.show(api_name, "Ready");
		} else if (func.equals("createDir")) {
			SyncProc.log.show(api_name, "Creating directory...");
			createDir(params.get("dirname"));
			SyncProc.log.show(api_name, "Ready");
		} else if (func.equals("deleteFile")) {
			SyncProc.log.show(api_name, "Deleting files...");
			deleteFile(params.get("filename"));
			SyncProc.log.show(api_name, "Ready");
		} else if (func.equals("refreshList")) {
			SyncProc.log.show(api_name, "Get file list from server...");
			refreshList();
			SyncProc.log.show(api_name, "Ready");
		}
	}
	
	public String extractName(String name) {
		for (int i = name.length() - 1; i >= 0; i--)
			if (name.charAt(i) == '/')
				return name.substring(i + 1);
		return name;
	}
	
	public String extractFolder(String name) {
		String folder_name = name;
		for (int i = name.length() - 1; i >= 0; i--)
			if (name.charAt(i) == '/') {
				return name.substring(0, i);
			}
		return folder_name;
	}
	
	public String getMimeType(String fullpath)
			throws java.io.IOException {
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String type = fileNameMap.getContentTypeFor("file://" + fullpath);

		return type;
	}
	
	public void initThread(Map<String, String> params) throws InterruptedException {
		thread = new MultiProcess(params);
	}
	
	public class MultiProcess extends Thread {
		public boolean success = true;
		public Semaphore semp;
		public Map<String, String> params;
		private String func;
		
		public MultiProcess(Map<String, String> params) throws InterruptedException {
			func = params.get("func");
			this.params = params;
			semp = new Semaphore(1);
			semp.acquire();
		}
		
		public void run() {
			for (int retry = 0; retry < Common.RETRY_TIMES; retry++) {
				try {
					callAPI(params);
					success = true;
					semp.release();
					return;
				} catch (Exception e) {
					System.out.println(api_name + "." + func + " error: " + e.getMessage());
					SyncProc.log.show(api_name, func + " error: " + e.getMessage());
				}				
			}
			success = false;
			semp.release();
		}
	}
}
