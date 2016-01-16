/*
 * Author:	周浩程
 * Date:	2013-1-30
 * E-Mail:	zhc105@163.com
 */

package MultiSyncAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.Session.AccessType;

import SyncMain.Common;
import SyncMain.SyncProc;

public class DropboxAPIs extends APIs {
	
	final static public String APP_KEY 		= "kfq1e2e1297bu7d";
	final static public String APP_SECRET	= "7f5x9ms6xa7gwn3";
	final static public AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	
	private String remote_path = "";
	private DropboxAPI<WebAuthSession> api;

	public DropboxAPIs() {
		api_name = "dropbox";
	}
	
	public void init(String sync_path, String remote_path, String token1, String token2) {
		this.remote_path = remote_path;
		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		WebAuthSession session = new WebAuthSession(appKeys, ACCESS_TYPE);
		session.setAccessTokenPair(new AccessTokenPair(token1, token2));
		api = new DropboxAPI<WebAuthSession>(session);
	}

	public void getFreeSpace() throws Exception {
		Account user = api.accountInfo(); 
		Long available = user.quota - user.quotaNormal - user.quotaShared;
		free_space = available;
		
		SyncProc.log.updateSpace(api_name, user.quotaNormal + user.quotaShared, user.quota);
	}

	public void downloadFile(String filename, String remotename) throws Exception {
		File localfile = new File(filename);
		OutputStream os = new FileOutputStream(localfile);
		api.getFile(remote_path + remotename, null, os, null);
		os.close();
	}

	public void uploadFile(String filename, String remotename)
			throws Exception {
		File localfile = new File(filename);
		InputStream is = new FileInputStream(localfile);
		api.putFileOverwrite(remote_path + remotename, is, localfile.length(), null);
		is.close();
		//System.out.println(res.path + "(" + res.bytes + " bytes)" + " finish");
	}

	public void createDir(String dirname) throws Exception {
		Entry res = api.createFolder(remote_path + dirname);
		System.out.println(res.path + " finish");
	}

	public void deleteFile(String filename) throws Exception {
		api.delete(remote_path + filename);
	}

	public String getFileContent(String filename) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			api.getFile(remote_path + filename, null, os, null);
		} catch (DropboxException e) {
			if (e.toString().contains("404 Not Found"))
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
	
	public void refreshList() throws Exception {
		root_list.clear();
		Entry entry = api.metadata(remote_path, 0, null, true, null);
		List<Entry> files = entry.contents;
		for (Entry file : files) {
			root_list.add(file.fileName());
		}
	}
}
