/*
 * Author:	周浩程
 * Date:	2013-1-30
 * E-Mail:	zhc105@163.com
 */

package MultiSyncAPI;

import com.baidu.api.domain.FileItem;
import com.baidu.api.utils.HttpUtil;
import com.baidu.api.utils.JsonUtil;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.TreeMap;

import SyncMain.Common;
import SyncMain.SyncProc;

public class BaiduAPIs extends APIs {
	public static final String API_KEY		= "GgI16Ahh3BMCbtfLDMhIKBkC";
	public static final String SECRET_KEY	= "pPQK95mGNzzSpBLUWHgo6M2azZvlG2aU";
	private static final String DEFAULT_CHARSET = "UTF-8";
	private static final String API_DOWNLOAD 	= "https://d.pcs.baidu.com/rest/2.0/pcs/file";
	private static final String API_URL 		= "https://pcs.baidu.com/rest/2.0/pcs/file";
	private static final int CONN_TIMEOUT 		= 20000;

	private String remote_path		= "";
	private String access_token		= "";
	private String refresh_token	= "";

	public BaiduAPIs() {
		api_name = "baidu";
	}

	public void init(String sync_path, String remote_path, String token1, String token2) {
		this.remote_path = remote_path;
		access_token = token1;
		refresh_token = token2;

	}

	public void getFreeSpace() throws IOException {
		String quota_url = "https://pcs.baidu.com/rest/2.0/pcs/quota";
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("method", "info");
		params.put("access_token", access_token);
		String result = "";
		try {
			result = HttpUtil.doGet(quota_url, params);
		} catch ( IOException e ) {
			JSONObject obj = JsonUtil.parseJson(e.getMessage());
			if (obj.containsKey("error_code") && obj.get("error_code").toString().equals("110")) {
				tokenRefresh();
				getFreeSpace();
				return;
			}
			else
				throw e;
		}
		JSONObject jsonobj = JsonUtil.parseJson(result);
		long quota = Long.parseLong(jsonobj.get("quota").toString());
		long used = Long.parseLong(jsonobj.get("used").toString());
		free_space = quota - used;

		SyncProc.log.updateSpace(api_name, used, quota);
	}

	public void downloadFile(String filename, String remotename) throws IOException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("method", "download");
		params.put("access_token", access_token);
		params.put("path", remote_path + remotename);
		HttpUtil.Download(API_URL, params, filename);
	}

	public void uploadFile(String filename, String remotename)
			throws Exception {
		String url = API_URL + "?method=upload&ondup=overwrite&access_token="
				+ access_token + "&path="
				+ URLEncoder.encode(remote_path + remotename, DEFAULT_CHARSET);
		HashMap<String, FileItem> fileParams = new HashMap<String, FileItem>();
		FileItem fileValue = new FileItem(filename);
		fileValue.setFileName(extractName(remotename));
		fileParams.put("file", fileValue);
		HttpUtil.uploadFile(url, null, fileParams, CONN_TIMEOUT, 300000);
	}

	public void createDir(String dirname) throws Exception {
		String url = API_URL + "?method=mkdir&access_token=" + access_token
				+ "&path="
				+ URLEncoder.encode(remote_path + dirname, DEFAULT_CHARSET);
		String result = HttpUtil.doPost(url, null);
		System.out.println("Result: " + result);
	}

	public void deleteFile(String filename) throws Exception {
		String url = API_URL + "?method=delete&access_token=" + access_token
				+ "&path="
				+ URLEncoder.encode(remote_path + filename, DEFAULT_CHARSET);
		String result = HttpUtil.doPost(url, null);
		System.out.println("Result: " + result);
	}

	public String getFileContent(String filename) throws Exception {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("method", "download");
		params.put("access_token", access_token);
		params.put("path", remote_path + filename);
		String result = null;
		try {
			result = HttpUtil.doGet(API_URL, params);
		} catch (IOException e) {
			JSONObject obj = JsonUtil.parseJson(e.getMessage());
			if (obj.containsKey("error_code") && obj.get("error_code").toString().equals("31066"))
				return ""; // file does not exist
			else
				throw e;
		}
		return result;
	}

	public void tokenRefresh() throws IOException {
		String url = "https://openapi.baidu.com/oauth/2.0/token";
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("client_id", API_KEY);
		params.put("client_secret", SECRET_KEY);
		params.put("refresh_token", refresh_token);
		params.put("grant_type", "refresh_token");
		params.put("scope", "netdisk");
		String result = HttpUtil.doGet(url, params);
		JSONObject jsonobj = JsonUtil.parseJson(result);
		access_token = jsonobj.get("access_token").toString();
		refresh_token = jsonobj.get("refresh_token").toString();

		/*update config file*/
		TreeMap<String, String> param_table = new TreeMap<String, String>();
		Common.getConfig(param_table);
		param_table.put("baidu_token1", access_token);
		param_table.put("baidu_token2", refresh_token);
		Common.putConfig(param_table);
	}

	@Override
	public void refreshList() throws Exception {
		root_list.clear();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("method", "list");
		params.put("access_token", access_token);
		params.put("path", remote_path);
		String json = "";
		try {
			json = HttpUtil.doGet(API_URL, params);
		} catch ( IOException e ) {
			JSONObject obj = JsonUtil.parseJson(e.getMessage());
			if (obj.containsKey("error_code") && obj.get("error_code").toString().equals("110")) {
				tokenRefresh();
				refreshList();
				return;
			}
			else
				throw e;
		}
		JSONObject jsonobj = JsonUtil.parseJson(json);
		Object[] objs = ((JSONArray) jsonobj.get("list")).toArray();
		for (int i = 0; i < objs.length; i++) {
			JSONObject file = (JSONObject) objs[i];
			root_list.add(extractName(file.get("path").toString()));
		}
	}

}
