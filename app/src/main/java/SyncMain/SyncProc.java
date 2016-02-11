/*
 * Author:	周浩程
 * Date:	2013-3-8
 * E-Mail:	zhc105@163.com
 */

package SyncMain;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import MD5Util.MD5FileUtil;
import MultiSyncAPI.APIs;
import MultiSyncAPI.BaiduAPIs;
import MultiSyncAPI.BoxAPIs;
import MultiSyncAPI.DBankAPIs;
import MultiSyncAPI.DropboxAPIs;
import MultiSyncAPI.GDriveAPIs;
import MultiSyncAPI.KuaipanAPIs;
import MultiSyncAPI.SkydriveAPIs;

import static android.os.Process.killProcess;

public class SyncProc {
	public static TreeMap<String, APIs> service;
	public static int interval = 30;
	public static String sync_path = "";
	public static boolean pause = false;
	public static StatusLogger log;
	private static ArrayList<String> deleted_list;
	private static TreeMap<String, FileDetails> local_index;
	private static TreeMap<String, FileDetails> server_index;
	private static ArrayList<APIs> usable_srv;
	private static boolean server_changed;
	private static boolean local_changed;

//	private static HashMap<String,Long> trafficAssign = new HashMap<String, Long>();


	public static void main(StatusLogger logger) throws Exception {
		log = logger;
		trafficAware();
//		updateFreeSpace();
		syncMain();
	}

	public static void syncMain() throws Exception {

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss") ;
		FileWriter out = null ;

		String fileName = "Download/output.txt";
		File file = new File(Environment.getExternalStorageDirectory(),fileName);
		if (file.exists()){
			out = new FileWriter(file,true);
		}else {
			file.createNewFile();
			out = new FileWriter(file,true);
		}

		String date ;
		int localTimes = 5;
		while (true) {
			try {
				while (pause) {
					log.show(null, "Paused");
					Thread.sleep(1000);
				}

				server_changed = false;
				local_changed = false;

				//记录时间
				date = simpleDateFormat.format(new java.util.Date()) ;
				long start = System.currentTimeMillis();


				log.show("", "Refreshing local file list...");
				refreshList();

				if (!server_changed && localTimes < 5) {
					log.show(null, "Synced");
					++localTimes;
					Thread.sleep(interval * 1000);
					continue;
				}
				localTimes = 0;

				log.show(null, "Refreshing server file list...");
				refreshServerList();

				log.show(null, "Downloading file index...");

				//上传的时候将该代码注释,不获取服务器端的index
				getServerIndex();
				log.show(null, server_index.size() + " files on server");

				updateServer();
				updateLocal();

				if (server_changed || local_changed)
					generateIndexfile();

				if (server_changed) {
					log.show(null, "Updating server index...");
					updateServerList();
				}

				log.show(null, "Synced");
				long end = System.currentTimeMillis();
				long time = end - start;

				String save = String.format("%s \t %d\n",date,time);
				out.write(save);
				out.close();
				//同步完成则关闭
				killProcess(android.os.Process.myPid());

			} catch (Exception e) {
				log.show(null, "Synchronize error: " + e.getMessage());
			}

			Thread.sleep(interval * 1000);
		}
	}


	private static void generateIndexfile() throws IOException {
		for (String fn : deleted_list) {
			server_index.remove(fn);
		}

		File index_file = new File(Common.HOME_PATH + Common.INDEX_FILENAME);
		BufferedWriter fout = new BufferedWriter(new FileWriter(index_file));
		for (Entry<String, FileDetails> entry : server_index.entrySet()) {
			FileDetails fd = entry.getValue();
			String line = entry.getKey() + ":" + fd.time_stamp + ":" + fd.MD5String + ":";
			line += convertParts(fd.Parts);
			fout.write(line + "\n");
		}
		fout.write("\n");
		fout.close();
	}

	private static void updateServerList() throws Exception {
		boolean success = true;
		/*upload to server*/
		for (Entry<String, APIs> entry : service.entrySet()) {
			APIs api = entry.getValue();
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("func", "uploadFile");
			params.put("filename", Common.HOME_PATH + Common.INDEX_FILENAME);
			params.put("remotename", Common.INDEX_FILENAME);
			api.initThread(params);
			api.thread.start();
			api.thread.semp.acquire();
			api.thread.semp.release();
		}
		for (Entry<String, APIs> entry : service.entrySet()) {
			APIs api = entry.getValue();
			api.thread.semp.acquire();
			api.thread.semp.release();
			if (!api.thread.success)
				success = false;
		}
		if (!success)
			throw new Exception("Failed to upload index file");
	}

	private static void updateLocal() throws Exception {
		File file ;

		boolean success = true;
		for (Entry<String, FileDetails> entry : server_index.entrySet())
			if (CheckLocalFile(entry.getKey(), entry.getValue().time_stamp))
				if (entry.getKey().charAt(0) != '$') {
					log.show(null, "Downloading \"" + entry.getKey() + "\"...");
					FileDetails fd = entry.getValue();
					for (int part = 1; part <= fd.Parts.length; part++) {
						APIs api = service.get(fd.Parts[part - 1]);
						if (api == null)
							throw new Exception("no such service: " + fd.Parts[part - 1]);
						HashMap<String, String> params = new HashMap<String, String>();
						params.put("func", "downloadFile");
						params.put("filename", Common.HOME_PATH + fd.MD5String + ".sp" + part);
						params.put("remotename", fd.MD5String + ".sp" + part);
						api.initThread(params);
						api.thread.start();
						api.thread.semp.acquire();
						api.thread.semp.release();
					}
					File target = new File(sync_path + entry.getKey());
					target.createNewFile();
					BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(target));
					for (int part = 1; part <= fd.Parts.length; part++) {
						APIs api = service.get(fd.Parts[part - 1]);
						api.thread.semp.acquire();
						api.thread.semp.release();
						File source = new File(Common.HOME_PATH + fd.MD5String + ".sp" + part);
						if (api.thread.success) {
							BufferedInputStream fin = new BufferedInputStream(
									new FileInputStream(source)
							);
							copyStream(fout, fin, source.length());
							fin.close();
						} else {
							success = false;
						}
						source.delete();
						target.delete() ;
					}
					fout.close();
					if (!success) {
						target.delete();
						throw new Exception("failed to download file");
					}
					target.setLastModified(fd.time_stamp);
					local_index.put(entry.getKey(), entry.getValue());
				} else {
					File dir = new File(sync_path + entry.getKey().substring(1));
					dir.mkdirs();
					local_index.put(entry.getKey(), entry.getValue());
				}
	}

	private static boolean CheckLocalFile(String filename, long modifier_time) {
		if (local_index.containsKey(filename)) { // local path contain this file
			FileDetails fd = local_index.get(filename);
			if (modifier_time / 1000 <= fd.time_stamp / 1000)
				return false; // file on the server was the older one
		} else {
			if (deleted_list.contains(filename))
				return false; // this file have been deleted
		}
		local_changed = true;
		return true;
	}

	private static void updateServer() throws Exception {
		for (Entry<String, FileDetails> entry : local_index.entrySet()) {
			String filename = entry.getKey();
			FileDetails local_fd = entry.getValue(), server_fd;
			if (((server_fd = server_index.get(filename)) == null)
					|| local_fd.time_stamp / 1000 > server_fd.time_stamp / 1000) {
				// upload file
				if (filename.charAt(0) != '$') {
					if (optDispatch(entry)) {
						server_index.put(entry.getKey(), entry.getValue());
						log.show(null, "Done!");
					}
				} else {
					server_index.put(entry.getKey(), entry.getValue());
				}
				server_changed = true;
			}
		}
	}

	private static boolean optDispatch(Entry<String, FileDetails> entry) throws Exception {
		double design ;
		DecimalFormat df   = new DecimalFormat("######0.00");
		boolean success = true;
		String fn = entry.getKey();
		FileDetails fd = entry.getValue();
		File f = new File(sync_path + fn);
		log.show(null, "Prepare to upload " + fn + "...");
		if (f.length() <= 0)
			return true;

		// if file has uploaded, return true
		for (Entry<String, FileDetails> srv_entry : server_index.entrySet()) {
			FileDetails sfd = srv_entry.getValue();
			if (sfd.MD5String.equals(fd.MD5String)) {
				fd.Parts = sfd.Parts;
				return true;
			}

		}

		//if file's size less than split_size, it will not split
		if (f.length() < Common.SPILIT_SIZE) {
			for (Entry<String, APIs> api_entry : service.entrySet()) {
				try {
					APIs api = api_entry.getValue();
					api.uploadFile(sync_path + fn, fd.MD5String + ".sp1");
					fd.Parts = new String[] { api.api_name };
					break;
				} catch ( Exception e ) {}
			}
		} else {
			log.show(null, "Collecting service status...");
//			updateFreeSpace();
			HashMap<APIs, Long> dispatch = new HashMap<>();
			Long remain = f.length();


			//calculate the full probe time and prepare for design
			Long probeTime = 0L;
			for (int i = 0; i < usable_srv.size(); i++) {
				APIs api = usable_srv.get(i);
				probeTime+=api.probeTime;
			}



			for (int i = 0; i < usable_srv.size(); i++){
//				if (usable_srv.get(i).free_space > Common.RESERVE_SPACE) {
					APIs api = usable_srv.get(i);
					Long disp;
//					disp = Math.min(api.free_space - Common.RESERVE_SPACE,
//							remain / (usable_srv.size() - i));
				if (i<usable_srv.size()-1) {
					double design1 = (api.probeTime * 1.0) / (probeTime * 1.0);
					design = Double.valueOf(df.format(design1));
					disp = (long) (design * f.length());
//					System.out.println("disp "+i+" "+disp);
					dispatch.put(api, disp);
					remain -= disp;
				}else {
					disp = remain ;
					dispatch.put(api,disp);
//					System.out.println("disp " + i + " " + disp);
					remain = 0L;
				}
			}
			if (remain > 0)
				throw new Exception("no enough free space");
//
//			for (Map.Entry<APIs,Long> d:dispatch.entrySet()){
//				APIs api = d.getKey();
//				System.out.println(api.api_name);
//				System.out.println(d.getValue());
//			}

			/* split file */
			log.show(null, "Uploading " + fn);
			BufferedInputStream fin = new BufferedInputStream(new FileInputStream(f));
			int idx = 1;
			ArrayList<String> parts = new ArrayList<>();
			for (Entry<APIs, Long> disp_entry : dispatch.entrySet()) {
				File part = new File(Common.HOME_PATH + fd.MD5String + ".sp" + idx);
				BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(part));
				copyStream(fout, fin, disp_entry.getValue());
				fout.close();
				/*upload*/
				HashMap<String, String> params = new HashMap<>();
				String server_fn = fd.MD5String + ".sp" + idx;
				params.put("func", "uploadFile");
				params.put("filename", Common.HOME_PATH + server_fn);
				params.put("remotename", server_fn);
				APIs api = disp_entry.getKey();
				System.out.println("Uploading " + server_fn + " to " + api.api_name + "...");
				api.initThread(params);
				api.thread.start();
				api.thread.semp.acquire();
				api.thread.semp.release();
				++idx;
			}
			fin.close();
			for (Entry<APIs, Long> disp_entry : dispatch.entrySet()) {
				APIs api = disp_entry.getKey();
				api.thread.semp.acquire();
				api.thread.semp.release();
				if (!api.thread.success) {
					success = false;
				} else  {
					parts.add(api.api_name);
					System.out.println(api.api_name + " Finish.");
				}
			}
			for (idx = 1; idx <= dispatch.size(); idx++) {
				File del_file = new File(Common.HOME_PATH + fd.MD5String + ".sp" + idx);
				del_file.delete();
			}
			fd.Parts = new String[parts.size()];
			parts.toArray(fd.Parts);
		}
		return success;
	}

	private static void getServerIndex() throws Exception {
		server_index.clear();
		boolean success = false;
		for (Entry<String, APIs> entry : service.entrySet()) {
			APIs api = entry.getValue();
			String result = "";
			try {
				if (api.root_list.contains(Common.INDEX_FILENAME)) {
					log.show(api.api_name, "Downloading index file...");
					result = api.getFileContent(Common.INDEX_FILENAME);
					log.show(api.api_name, "Done.");
				}
			} catch (Exception e) {
				System.out.println(api.api_name + ".getFileContent error: "
						+ e.getMessage());
				log.show(api.api_name, "getFileContent error: " + e.getMessage());
				continue;
			}
			result = result.replaceAll("\r\n", "\n");
			String[] liststr = result.split("\n");
			for (int i = 0; i < liststr.length; i++) {
				String[] tmp = liststr[i].split(":");
				if (tmp.length >= 3) {
					String[] parts;
					if (tmp.length >= 4)
						parts = tmp[3].split(",");
					else
						parts = new String[] {};
					server_index.put(tmp[0],
							new FileDetails(Long.parseLong(tmp[1]), // file name
									tmp[2], // timestamp
									parts // file parts
							));
				}
			}
			success = true;
			break;
		}

		if (!success)
			throw new Exception("cannot download index file");
	}

	private static void updateFreeSpace() throws Exception {
		for (Entry<String, APIs> entry : service.entrySet()) {
			APIs api = entry.getValue();
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("func", "getFreeSpace");
			api.initThread(params);
			api.thread.start();
			api.thread.semp.acquire(); // wait for calling finish
			api.thread.semp.release();
		}
		usable_srv.clear();
		for (Entry<String, APIs> entry : service.entrySet()) {
			APIs api = entry.getValue();
			api.thread.semp.acquire(); // wait for calling finish
			api.thread.semp.release();
			if (api.thread.success && api.free_space > Common.RESERVE_SPACE) {
				System.out.printf("%-10s%20s\n", api.api_name,
						String.valueOf(api.free_space));
				usable_srv.add(api);
			}
		}
		/* sort by free space size */
		Comparator<APIs> comp = new Comparator<APIs>() {
			public int compare(APIs o1, APIs o2) {
				if (o1.free_space > o2.free_space)
					return 1;
				return 0;
			}
		};
		Collections.sort(usable_srv, comp);
	}

	private static void trafficAware() throws InterruptedException, IOException {

		String str = "012345678vasdjhklsadfqwiurewopt";
		String fileTemp = "temp";

		File temp = new File(sync_path + fileTemp);
		if (temp.exists())
			temp.delete();
		temp.createNewFile();

		PrintWriter pw = new PrintWriter(new FileWriter(temp));
		int len = str.length();
		int loop = 10;
		for (int i = 0; i < loop; i++) {
			StringBuilder s = new StringBuilder();
			for (int j = 0; j < 1024; j++) {
				s.append(str.charAt((int) (Math.random() * len)));
			}
			pw.println(s.toString());
		}
		pw.close();

		long start;
		long period;
//		String apiName;
		for (Entry<String, APIs> api_entry : service.entrySet()) {
			try {
				APIs api = api_entry.getValue();
//				apiName = api.api_name;
				start = System.currentTimeMillis();
				api.uploadFile(sync_path + fileTemp, fileTemp);
				period = System.currentTimeMillis() - start;
//				trafficAssign.put(apiName,period);
				api.probeTime = period ;
//				System.out.println(api.probeTime);

			} catch (Exception e) {
			}
		}
		if (temp.exists())
			temp.delete();
	}


	public static void loadConfig(StatusLogger log) throws Exception {

		HashMap<String, String> param_table = new HashMap<String, String>();
		Common.getConfig(param_table);

		sync_path = param_table.get("syncpath");
		interval = Integer.parseInt(param_table.get("synctime"));
		service = new TreeMap<>();

		sync_path = sync_path.replaceAll("\\\\", "/");
		if (sync_path.length() == 0
				|| sync_path.charAt(sync_path.length() - 1) != '/')
			sync_path += "/";

		File spath_file = new File(sync_path);
		spath_file.mkdirs();	// create dir if not exist

		for (int i = 0; i < Common.SERVICES.length; i++) {
			if (param_table.containsKey(Common.SERVICES[i] + "_token1") && param_table.get(Common.SERVICES[i] + "_token1") != "") {
				String path = param_table.get(Common.SERVICES[i] + "_path");
				String token1 = param_table.get(Common.SERVICES[i] + "_token1");
				String token2 = param_table.get(Common.SERVICES[i] + "_token2");

				if (token1.trim().length() == 0)
					continue;

				APIs api = null;
				if (Common.SERVICES[i].equals("dropbox")) {
					api = new DropboxAPIs();
				} else if (Common.SERVICES[i].equals("kuaipan")) {
					api = new KuaipanAPIs();
				} else if (Common.SERVICES[i].equals("baidu")) {
					api = new BaiduAPIs();
				} else if (Common.SERVICES[i].equals("gdrive")) {
					api = new GDriveAPIs();
				} else if (Common.SERVICES[i].equals("skydrive")) {
					api = new SkydriveAPIs();
				} else if (Common.SERVICES[i].equals("dbank")) {
					api = new DBankAPIs();
				} else if (Common.SERVICES[i].equals("box")) {
					api = new BoxAPIs();
				}

				path = path.replaceAll("\\\\", "/");
				if (path.length() == 0 || path.charAt(path.length() - 1) != '/')
					path += "/";

				if (api != null) {
					api.init(sync_path, path, token1, token2);
					service.put(api.api_name, api);
				}
			}
		}

		/* initialize file list */
		server_index = new TreeMap<String, FileDetails>();
		deleted_list = new ArrayList<String>();
		local_index = new TreeMap<String, FileDetails>();
		try {
			File index_file = new File(Common.HOME_PATH + Common.INDEX_FILENAME);
			BufferedReader br = new BufferedReader(new FileReader(index_file));
			String line;
			/*read old local index*/
			while ((line = br.readLine()) != null) {
				String[] tmp = line.split(":");
				if (tmp.length >= 4) {
					String[] parts = tmp[3].split(",");
					local_index.put(tmp[0],
							new FileDetails(Long.parseLong(tmp[1]), // file name
									tmp[2], // timestamp
									parts // file parts
							));
				}
			}
			br.close();
		} catch ( Exception e ) {}

		usable_srv = new ArrayList<APIs>();
		
		/* add services */
		for (Entry<String, APIs> entry : service.entrySet()) {
			log.addService(entry.getKey());
			usable_srv.add(entry.getValue());
		}
	}

	private static TreeMap<String, FileDetails> getLocalIndex(String path) throws IOException {
		File dir = new File(path);
		File[] files = dir.listFiles();
		TreeMap<String, FileDetails> ret_index = new TreeMap<String, FileDetails>();

		if (files == null)
			return ret_index;

		for (int i = 0; i < files.length; i++) {
			String filename = getRelativePath(files[i].getAbsolutePath());
			if (files[i].isDirectory()) {
				filename = filename.replaceAll("\\\\", "/");
				if (filename.charAt(filename.length() - 1) != '/')
					filename += "/";
				FileDetails fd = new FileDetails(
						0,
						"0",
						new String[] {}
				);
				ret_index.put("$" + filename, fd);
				ret_index.putAll(getLocalIndex(sync_path + filename));
			} else {
				if (local_index.containsKey(filename) &&
						local_index.get(filename).time_stamp / 1000 == files[i].lastModified() / 1000) {
					ret_index.put(filename, local_index.get(filename));
				} else {
					FileDetails fd = new FileDetails(
							files[i].lastModified(),
							MD5FileUtil.getFileMD5String(files[i]),
							new String[] {}
					);
					ret_index.put(filename, fd);
				}

			}
		}
		return ret_index;
	}

	private static void refreshServerList() throws Exception {
		boolean success = true;
		for (Entry<String, APIs> entry : service.entrySet()) {
			APIs api = entry.getValue();
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("func", "refreshList");
			api.initThread(params);
			api.thread.start();
			api.thread.semp.acquire(); // wait for calling finish
			api.thread.semp.release();
		}
		for (Entry<String, APIs> entry : service.entrySet()) {
			APIs api = entry.getValue();
			api.thread.semp.acquire(); // wait for calling finish
			api.thread.semp.release();
			if (!api.thread.success)
				success = false;
		}
		if (!success)
			throw new Exception("refreshServerList error.");
	}

	private static void refreshList() throws IOException {
		TreeMap<String, FileDetails> new_list = getLocalIndex(sync_path);
		deleted_list.clear();
		for (Entry<String, FileDetails> entry : local_index.entrySet())
			if (!new_list.containsKey(entry.getKey())) {
				deleted_list.add(entry.getKey());
				server_changed = true;
			}
		for (Entry<String, FileDetails> entry : new_list.entrySet())
			if (!local_index.containsKey(entry.getKey())) {
				server_changed = true;
				break;
			} else {
				FileDetails fd = local_index.get(entry.getKey());
				if (entry.getValue().time_stamp / 1000 > fd.time_stamp / 1000) {
					server_changed = true;
					break;
				}
			}
		local_index = new_list;
	}

	private static void copyStream(OutputStream os, InputStream is, Long length) throws IOException {
		byte[] buf = new byte[Common.BUFFER_SIZE];
		int len;
		while (length > 0) {
			int size = (int) Math.min(Common.BUFFER_SIZE, length);
			len = is.read(buf, 0, size);
			if (len <= 0)
				throw new IOException("an error occured while reading file");
			os.write(buf, 0, len);
			length -= len;
		}
	}

	private static String getRelativePath(String path) {
		try {
			int len = sync_path.length();
			path = path.replaceAll("\\\\", "/");
			String tmp = path.substring(0, len);
			if (tmp.equals(sync_path))
				return path.substring(len);
			else
				return path;
		} catch (Exception e) {
			return null;
		}
	}

	private static String convertParts(String[] parts) {
		String ret = "";
		if (parts.length > 0) {
			ret += parts[0];
			for (int i = 1; i < parts.length; i++)
				ret += "," + parts[i];
		}
		return ret;
	}



}

class FileDetails {
	public String[] Parts;
	public String MD5String;
	public long time_stamp;

	public FileDetails(long time_stamp, String MD5, String[] parts) {
		this.time_stamp = time_stamp;
		this.MD5String = MD5;
		this.Parts = parts;
	}
}

