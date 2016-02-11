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
import java.util.HashMap;
import java.util.TreeMap;

import SyncMain.Common;
import SyncMain.SyncProc;

public class BoxAPIs extends APIs {
	public static final String CLIENT_ID		= "8t0n3dy7jew1kagr7ce3d272u8i8klsp";
	public static final String CLIENT_SECRET	= "rIAaRpENAWBGLyaWTNSIbnHQLdyGGTiX";
	private static final String API_URL 		= "https://api.box.com/2.0";
	private static final String DEFAULT_CHARSET = "UTF-8";
	private static final int CONN_TIMEOUT 		= 20000;

	private String remote_path		= "";
	private String access_token		= "";
	private String refresh_token	= "";
	private String author_string	= "";

	private String root_id 	= "0";
	private HashMap<String, String> name2id = new HashMap<String, String>();

	public BoxAPIs() {
		api_name = "box";
	}

	public void init(String sync_path, String remote_path, String token1, String token2) {
		this.remote_path = remote_path;
		this.access_token = token1;
		this.refresh_token = token2;
		this.author_string = "Bearer " + access_token;
	}

	public void getFreeSpace() throws IOException {
		String url = API_URL + "/users/me";
		String result = "";
		try {
			result = HttpUtil.doGetAuthorize(url, null, author_string);
		} catch (IOException e) {
			if (e.getMessage().equals("401")) {
				tokenRefresh();
				result = HttpUtil.doGetAuthorize(url, null, author_string);
			} else {
				throw e;
			}
		}
		JSONObject jsonobj = JsonUtil.parseJson(result);
		long quota = Long.parseLong(jsonobj.get("space_amount").toString());
		long used  = Long.parseLong(jsonobj.get("space_used").toString());

		free_space = quota - used;
		SyncProc.log.updateSpace(api_name, used, quota);
	}

	public void downloadFile(String filename, String remotename) throws IOException {
		String fid = getFileID(remote_path + remotename);
		if (fid == null)
			throw new IOException("No such file '" + filename + "'");
		String url = API_URL + "/files/" + fid + "/content";
		HttpUtil.DownloadAuthorize(url, null, filename, author_string);
	}

	public void uploadFile(String filename, String remotename)
			throws Exception {

		deleteFile(remotename);	//	try to delete existing file

		remotename = remote_path + remotename;
		String folder_id = getFolderID(remotename);
		String url = "https://upload.box.com/api/2.0/files/content";

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("parent_id", folder_id);
		HashMap<String, FileItem> fileParams = new HashMap<String, FileItem>();
		FileItem fileValue = new FileItem(filename);
		fileValue.setFileName(extractName(remotename));
		fileParams.put("file", fileValue);

		String result;
		try {
			result = HttpUtil.uploadFileAuthorize(url, params, fileParams, CONN_TIMEOUT, 300000, author_string);
		} catch (IOException e) {
			if (e.getMessage().equals("401")) {
				tokenRefresh();
				result = HttpUtil.uploadFileAuthorize(url, params, fileParams, CONN_TIMEOUT, 300000, author_string);
			} else {
				throw e;
			}
		}
		JSONObject jsonobj = JsonUtil.parseJson(result);
		if (!jsonobj.containsKey("total_count") ||
				Integer.parseInt(jsonobj.get("total_count").toString()) <= 0) {
			throw new Exception("Upload error.");
		}
	}

	public void createDir(String dirname) throws Exception {

	}

	public void deleteFile(String filename) throws Exception {
		String remote_name = remote_path + filename;
		String fid = getFileID(remote_name);
		if (fid == null)
			return;
		String url = API_URL + "/files/" + fid;

		try {
			HttpUtil.doDeleteAuthorize(url, null, DEFAULT_CHARSET, author_string);
		} catch (IOException e) {
			if (e.getMessage().equals("401")) {
				tokenRefresh();
				HttpUtil.doDeleteAuthorize(url, null, DEFAULT_CHARSET, author_string);
			} else {
			}
		}
	}

	public String getFileContent(String filename) throws Exception {
		String result = "";
		String fid = getFileID(remote_path + filename);
		if (fid == null)
			throw new Exception("No such file '" + filename + "'");
		String url = API_URL + "/files/" + fid + "/content";

		try {
			result = HttpUtil.doGetAuthorize(url, null, author_string);
		} catch (IOException e) {
			if (e.getMessage().equals("401")) {
				tokenRefresh();
				result = HttpUtil.doGetAuthorize(url, null, author_string);
			} else {
				throw e;
			}
		}

		return result;
	}

	public void tokenRefresh() throws IOException {
		String url = "https://www.box.com/api/oauth2/token";
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("grant_type", "refresh_token");
		params.put("refresh_token", refresh_token);
		params.put("client_id", CLIENT_ID);
		params.put("client_secret", CLIENT_SECRET);
		String result = HttpUtil.doPost(url, params);
		JSONObject jsonobj = JsonUtil.parseJson(result);
		access_token = jsonobj.get("access_token").toString();
		refresh_token = jsonobj.get("refresh_token").toString();
		author_string = "Bearer " + access_token;
		
		/*update config file*/
		TreeMap<String, String> param_table = new TreeMap<String, String>();
		Common.getConfig(param_table);
		param_table.put("box_token1", access_token);
		param_table.put("box_token2", refresh_token);
		Common.putConfig(param_table);
	}

	@Override
	public void refreshList() throws Exception {
		root_list.clear();
		name2id.clear();
		name2id.put("", root_id);

		String result;
		String url = API_URL + "/folders/" + root_id;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("fields", "item_collection,name");
		params.put("limit", "1000");
		try {
			result = HttpUtil.doGetAuthorize(url, params, author_string);
		} catch (IOException e) {
			if (e.getMessage().equals("401")) {
				tokenRefresh();
				result = HttpUtil.doGetAuthorize(url, params, author_string);
			} else {
				throw e;
			}
		}
		JSONObject jsonobj = JsonUtil.parseJson(result);
		Object[] objs = ((JSONArray) ((JSONObject) jsonobj.get("item_collection")).get("entries")).toArray();
		for (int i = 0; i < objs.length; i++) {
			JSONObject file = (JSONObject) objs[i];
			root_list.add(file.get("name").toString());
			String cname = remote_path + file.get("name").toString();
			name2id.put(cname, file.get("id").toString());
		}
	}

	public String getFileID(String filename) throws IOException {
		return name2id.get(filename);
	}

	private String getFolderID(String name) throws IOException {
		String folder_name, folder_id = root_id;
		for (int i = name.length() - 1; i > 0; i--)
			if (name.charAt(i) == '/') {
				folder_name = name.substring(0, i);
				folder_id = getFileID(folder_name);
				break;
			}
		return folder_id;
	}
}
