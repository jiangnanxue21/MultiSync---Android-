/*
 * Author:	周浩程
 * Date:	2013-1-30
 * E-Mail:	zhc105@163.com
 */

package MultiSyncAPI;

import java.util.HashMap;
import java.util.Map;

import SyncMain.SyncProc;

import com.baidu.api.utils.HttpUtil;

import nsp.NSPClient;
import nsp.VFS;
import nsp.VFS.File;
import nsp.VFS.LsResult;
import nsp.VFSExt;
import nsp.VFS.Result;
import nsp.support.common.AssocArrayUtil;

public class DBankAPIs extends APIs {
	public static final String CONSUMER_KEY 	= "60520";
	public static final String CONSUMER_SECRET	= "nzkZwLEdyA4MdY4q4V64O7KiLDpYFj4Q";

	private String remote_path	= "";

	private NSPClient client;
	private VFS vfs;
	private VFSExt vfs_ext;

	public DBankAPIs() {
		api_name = "dbank";
	}
	
	public void init(String sync_path, String remote_path, String token1, String token2) {
		this.remote_path = remote_path;
		client = new NSPClient(token1, token2);
		vfs = new VFS(client);
		vfs_ext = new VFSExt(vfs);
	}

	@SuppressWarnings("unchecked")
	public void getFreeSpace() throws Exception {
		Object obj = client.callService("nsp.user.getInfo",
				new Object[] { new String[] { "product.spacecapacity",
						"profile.usedspacecapacity" , "product.fileuploadsize"} });
		Map<String, String> result = (Map<String, String>) AssocArrayUtil
				.toObject(obj, false);
		Long used = Long.parseLong(result.get("profile.usedspacecapacity"));
		Long total = Long.parseLong(result.get("product.spacecapacity"));
		Long single = Long.parseLong(result.get("product.fileuploadsize"));
		free_space = Math.min(total - used, single);
		
		SyncProc.log.updateSpace(api_name, used, total);
	}

	public void downloadFile(String filename, String remotename) throws Exception {
		Result res = vfs.getattr(new String[] { remote_path + remotename },
				new String[] { "url" });
		File[] files = res.getSuccessList();
		if (files.length < 1) {
			VFS.Error[] fail = res.getFailList();
			if (fail.length > 0)
				throw new Exception(fail[0].getErrMsg());
			else
				throw new Exception("Unknown exception");
		}
		String url = files[0].get("url").toString();
		HttpUtil.Download(url, null, filename);
	}

	public void uploadFile(String filename, String remotename)
			throws Exception {
		// remove old file;
		remotename = remote_path + remotename;
		vfs.rmfile(new String[] { remotename }, false, null); 
		java.io.File local_file = new java.io.File(filename);
		if (local_file.length() <= 0)
			return;
		Result res;
		res = vfs_ext.uploadFile(extractFolder(remotename), local_file, 
				extractName(remotename));
		File[] files = res.getSuccessList();
		if (files.length < 1) {
			VFS.Error[] fail = res.getFailList();
			if (fail.length > 0)
				throw new Exception(fail[0].getErrMsg());
			else
				throw new Exception("Unknown exception");
		}
	}

	public void createDir(String dirname) throws Exception {
		while (dirname.charAt(dirname.length() - 1) == '/')
			dirname = dirname.substring(0, dirname.length() - 1);
		dirname = remote_path + dirname;
		//remove old directory
		vfs.rmfile(new String[] { dirname }, false, null); 
		String dirpath = extractFolder(dirname);
		dirname = extractName(dirname);
		File dir = new File();
		dir.setType("Directory");
		dir.setName(dirname);
		Result res = vfs.mkfile(new File[] { dir }, dirpath);
		File[] files = res.getSuccessList();
		if (files.length < 1) {
			VFS.Error[] fail = res.getFailList();
			if (fail.length > 0)
				throw new Exception(fail[0].getErrMsg());
			else
				throw new Exception("Unknown exception");
		}
	}

	public void deleteFile(String filename) throws Exception {
		while (filename.charAt(filename.length() - 1) == '/')
			filename = filename.substring(0, filename.length() - 1);
		vfs.rmfile(new String[] { remote_path + filename }, false, null);
	}

	public String getFileContent(String filename) throws Exception {
		Result res = vfs.getattr(new String[] { remote_path + filename },
				new String[] { "url" });
		File[] files = res.getSuccessList();
		if (files.length < 1) {
			VFS.Error[] fail = res.getFailList();
			if (fail.length > 0) {
				if (fail[0].getErrCode() == 103)
					return "";
				else
					throw new Exception(fail[0].getErrMsg());
			} else
				throw new Exception("Unknown exception");
		}
		String url = files[0].get("url").toString();
		String result = HttpUtil.doGet(url, null);
		return result;
	}

	@Override
	public void refreshList() throws Exception {
		root_list.clear();
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put("recursive", 1);
		LsResult result = vfs.lsdir(remote_path, new String[] { "name" }, options);
		File[] files = result.getChildList();
		for (int i = 0; i < files.length; i++) {
			root_list.add(files[i].getName());
		}
	}

}
