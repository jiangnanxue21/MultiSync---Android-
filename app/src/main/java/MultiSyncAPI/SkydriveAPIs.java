/*
 * Author:	周浩程
 * Date:	2013-1-30
 * E-Mail:	zhc105@163.com
 */

package MultiSyncAPI;

import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.baidu.api.domain.FileItem;
import com.baidu.api.utils.HttpUtil;
import com.baidu.api.utils.JsonUtil;

import SyncMain.Common;
import SyncMain.SyncProc;

public class SkydriveAPIs extends APIs {
	public static final String CLIENT_ID 		= "00000000480E9B51";
	public static final String CLIENT_SECRET	= "1sCVFhU23HQnzAn6jaGwWZI55iFcpgr-";
	private static final String API_URL 		= "https://apis.live.net/v5.0/";
	private static final String ROOT_ID 		= "me/skydrive";
	private static final int CONN_TIMEOUT		= 20000;

	private String remote_path		= "";
	private String access_token	= "";
	private String refresh_token	= "";
	private String file_source		= "";

	public SkydriveAPIs() {
		api_name = "skydrive";
	}
	
	public void init(String sync_path, String remote_path, String token1, String token2) {
		this.remote_path	= remote_path;
		this.access_token	= token1;
		this.refresh_token	= token2;
	}
	
	public void getFreeSpace() throws IOException {
		try {
			String url = API_URL + ROOT_ID + "/quota";
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("access_token", access_token);
			String result = HttpUtil.doGet(url, params);
			JSONObject jsonobj = JsonUtil.parseJson(result);
			Long available = Long.parseLong(jsonobj.get("available").toString());
			free_space = available;
			
			Long total = Long.parseLong(jsonobj.get("quota").toString());
			Long used = total - available;
			SyncProc.log.updateSpace(api_name, used, total);
		} catch ( IOException e ) {
			if (isTokenExpired(e)) {
				tokenRefresh();
				getFreeSpace();
			} else 
				throw e;
		}
	}

	public void downloadFile(String filename, String remotename) throws IOException {
		getFileID(remote_path + remotename);
		String url = file_source;
		if (file_source.indexOf('?') != -1)
			url += "&AVOverride=1";
		else
			url += "?AVOverride=1";
		try {
			HttpUtil.Download(url, null, filename);
		} catch ( IOException e ) {
			if (isTokenExpired(e)) 
				tokenRefresh();
			throw e;
		}
	}

	public void uploadFile(String filename, String remotename) throws Exception {
		String folder_id = getFolderID(remote_path + remotename);

		String url = API_URL + folder_id + "/files?access_token="
				+ access_token;

		HashMap<String, FileItem> fileParams = new HashMap<String, FileItem>();
		FileItem fileValue = new FileItem(filename);
		fileValue.setFileName(extractName(remotename));
		fileParams.put("file", fileValue);
		try {
			HttpUtil.uploadFile(url, null, fileParams, CONN_TIMEOUT, 300000);
			//System.out.println("Result: " + result);
		} catch ( IOException e ) {
			if (isTokenExpired(e)) 
				tokenRefresh();
			throw e;
		}	
	}

	public void createDir(String dirname) throws Exception {
		while (dirname.charAt(dirname.length() - 1) == '/')
			dirname = dirname.substring(0, dirname.length() - 1);
		String folder_id = getFolderID(remote_path + dirname);

		String url = API_URL + folder_id + "?access_token=" + access_token;
		HashMap<String, String> name = new HashMap<String, String>();
		for (int i = dirname.length() - 1; i > 0; i--) // extract filename
			if (dirname.charAt(i) == '/') {
				dirname = dirname.substring(i + 1);
				break;
			}
		name.put("name", dirname);
		String result = JSONObject.toJSONString(name);
		try {
			result = HttpUtil.doPostJSON(url, result, Common.DEFAULT_CHARSET);
		} catch ( IOException e ) {
			if (isTokenExpired(e)) 
				tokenRefresh();
			throw e;
		}

		System.out.println("Result: " + result);
	}

	public void deleteFile(String filename) throws Exception {
		String url = API_URL + getFileID(remote_path + filename);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		try {
			String result = HttpUtil.doDelete(url, params, Common.DEFAULT_CHARSET);
			System.out.println("Result: " + result);
		} catch ( IOException e ) {
			if (isTokenExpired(e)) 
				tokenRefresh();
			throw e;
		}
	}

	public String getFileContent(String filename) throws Exception {
		String file_id = getFileID(remote_path + filename);
		String result = "";
		if (file_id != null) {
			String url = API_URL + file_id + "/content";
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("access_token", access_token);
			try {
				result = HttpUtil.doGet(url, params);
			} catch ( IOException e ) {
				if (isTokenExpired(e)) 
					tokenRefresh();
				throw e;
			}
		}
		return result;
	}
	
	public void tokenRefresh() throws IOException {
		String url = "https://login.live.com/oauth20_token.srf";
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("client_id", CLIENT_ID);
		params.put("client_secret", CLIENT_SECRET);
		params.put("refresh_token", refresh_token);
		params.put("grant_type", "refresh_token");
		String result = HttpUtil.doPost(url, params);
		JSONObject jsonobj = JsonUtil.parseJson(result);
		access_token = jsonobj.get("access_token").toString();
		refresh_token = jsonobj.get("refresh_token").toString();
		
		/*update config file*/
		TreeMap<String, String> param_table = new TreeMap<String, String>();
		Common.getConfig(param_table);
		param_table.put("skydrive_token1", access_token);
		param_table.put("skydrive_token2", refresh_token);
		Common.putConfig(param_table);
	}

	public String getFileID(String filename) throws IOException {

		while (filename.charAt(0) == '/')
			filename = filename.substring(1);

		String file_id = ROOT_ID;
		while (!filename.trim().equals("")) {
			String url = API_URL + file_id + "/files";
			String tmp;
			int next = filename.indexOf('/');
			if (next == -1) {
				tmp = filename;
				filename = "";
			} else {
				tmp = filename.substring(0, next);
				filename = filename.substring(next + 1);
			}

			HashMap<String, String> params = new HashMap<String, String>();
			params.put("access_token", access_token);
			String result = HttpUtil.doGet(url, params);
			JSONObject jsonobj = JsonUtil.parseJson(result);
			if (!jsonobj.containsKey("data"))
				throw new IOException(jsonobj.toString());
			Object[] objs = ((JSONArray) jsonobj.get("data")).toArray();
			boolean flag = false;
			for (int i = 0; i < objs.length; i++) {
				JSONObject file = (JSONObject) objs[i];
				if (file.get("name").toString().equals(tmp)) {
					flag = true;
					file_id = file.get("id").toString();
					if (file.containsKey("source"))
						file_source = file.get("source").toString();
					break;
				}
			}
			if (!flag)
				return null;
		}

		return file_id;
	}

	private String getFolderID(String name) throws IOException {
		String folder_name, folder_id = ROOT_ID;
		for (int i = name.length() - 1; i > 0; i--)
			if (name.charAt(i) == '/') {
				folder_name = name.substring(0, i);
				folder_id = getFileID(folder_name);
				break;
			}
		return folder_id;
	}
	
	private boolean isTokenExpired(IOException e) {
		JSONObject jsonobj = JsonUtil.parseJson(e.getMessage());
		if (jsonobj != null && jsonobj.containsKey("error")) {
			jsonobj = (JSONObject) jsonobj.get("error");
			if (jsonobj.get("code").toString().equals("request_token_expired")) 
				return true;
		}
		return false;
	}

	@Override
	public void refreshList() throws Exception {
		try {
			root_list.clear();
			String url = API_URL + ROOT_ID + "/files";
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("access_token", access_token);
			String result = HttpUtil.doGet(url, params);
			JSONObject jsonobj = JsonUtil.parseJson(result);
			if (!jsonobj.containsKey("data"))
				throw new IOException(jsonobj.toString());
			Object[] objs = ((JSONArray) jsonobj.get("data")).toArray();
			for (int i = 0; i < objs.length; i++) {
				JSONObject file = (JSONObject) objs[i];
				root_list.add(file.get("name").toString());
			}
		} catch ( IOException e ) {
			if (isTokenExpired(e)) {
				tokenRefresh();
				refreshList();
			} else 
				throw e;
		}
	}
}
