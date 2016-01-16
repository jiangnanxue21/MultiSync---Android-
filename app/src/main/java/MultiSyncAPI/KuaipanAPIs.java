/*
 * Author:	周浩程
 * Date:	2013-1-30
 * E-Mail:	zhc105@163.com
 */

package MultiSyncAPI;

import com.kuaipan.client.exception.KuaipanServerException;
import com.kuaipan.client.model.KuaipanFile;
import com.kuaipan.client.model.KuaipanUser;
import com.kuaipan.client.session.OauthSession;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import SyncMain.Common;
import SyncMain.SyncProc;

public class KuaipanAPIs extends APIs{
	public final static String CONSUMER_KEY = "xcH5JD7fq1JECdBE";
	public final static String CONSUMER_SECRET = "6gYkh9dmdeoXyHca";
	
	private String remote_path = "";
	private com.kuaipan.client.KuaipanAPI api;

	public KuaipanAPIs() {
		api_name = "kuaipan";
	}
	
	public void init(String sync_path, String remote_path, String token1, String token2) {
		this.remote_path = remote_path;
		OauthSession session = new OauthSession(CONSUMER_KEY, CONSUMER_SECRET, 
				OauthSession.Root.APP_FOLDER);
		session.setAuthToken(token1, token2);
		api = new com.kuaipan.client.KuaipanAPI(session);
	}

	public void getFreeSpace() throws Exception {
		KuaipanUser user = api.accountInfo();
		Long available = Math.min(user.max_file_size, user.quota_total - user.quota_used);
		free_space = available;
		
		SyncProc.log.updateSpace(api_name, user.quota_used, user.quota_total);
	}

	public void downloadFile(String filename, String remotename) throws Exception {
		File localfile = new File(filename);
		OutputStream os = new FileOutputStream(localfile);
		api.downloadFile(remote_path + remotename, os, null);
		os.close();
	}

	public void uploadFile(String filename, String remotename)
			throws Exception {
		File localfile = new File(filename);
		InputStream is = new FileInputStream(localfile);
		api.uploadFile(remote_path + remotename, is, localfile.length(), true, null);
		is.close();
		//System.out.println(res);
	}

	public void createDir(String dirname) throws Exception {
		KuaipanFile res = api.createFolder(remote_path + dirname);
		System.out.println(res);
	}

	public void deleteFile(String filename) throws Exception {
		KuaipanFile res = api.delete(remote_path + filename);
		System.out.println(res);
	}

	public String getFileContent(String filename) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			api.downloadFile(remote_path + filename, os, null);
		} catch (KuaipanServerException e) {
			if (e.code == 403 || e.code == 404)
				return "";
			else
				throw e;
		}
		String download_content = os.toString(Common.DEFAULT_CHARSET);
		try {
			os.close();
		} catch (IOException e) {}
		return download_content;
	}

	@Override
	public void refreshList() throws Exception {
		root_list.clear();
		KuaipanFile file = api.metadata(remote_path, true);		
		for (Iterator<KuaipanFile> it=file.files.iterator(); it.hasNext();) {
			KuaipanFile temp = it.next();
			root_list.add(temp.name);
		}
	}
}
