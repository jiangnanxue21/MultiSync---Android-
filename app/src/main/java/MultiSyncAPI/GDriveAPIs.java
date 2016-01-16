/*
 * Author:	周浩程
 * Date:	2013-2-8
 * E-Mail:	zhc105@163.com
 */

package MultiSyncAPI;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

import SyncMain.Common;
import SyncMain.SyncProc;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

public class GDriveAPIs extends APIs{
	public static String CLIENT_ID = "886570393339.apps.googleusercontent.com";
	public static String CLIENT_SECRET = "_JfIaYvkp4kM6xqo_QFd9MtI";

	private static final int LIST_MAXSIZE = 10485760;

	private String remote_path = "";
	private String root_id = null;

	public HttpTransport httpTransport = new NetHttpTransport();;
	public JsonFactory jsonFactory = new JacksonFactory();
	public Drive service;
	public Map<String, String> name2id = new HashMap<String, String>();
	public Map<String, String> id2name = new HashMap<String, String>();
	public About about;

	public GDriveAPIs() {
		api_name = "gdrive";
	}
	
	public void init(String sync_path, String remote_path, String token1, String token2) throws IOException {
		this.remote_path = remote_path;
		GoogleCredential credential = new GoogleCredential.Builder()
				.setClientSecrets(CLIENT_ID, CLIENT_SECRET)
				.setJsonFactory(jsonFactory).setTransport(httpTransport)
				.build().setAccessToken(token1)
				.setRefreshToken(token2);
		service = new Drive.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName("MultiSync").build();
	}

	public void getFreeSpace() throws IOException {
		about = service.about().get().execute();
		free_space = about.getQuotaBytesTotal() - about.getQuotaBytesUsed();
		root_id = about.getRootFolderId();
		
		SyncProc.log.updateSpace(api_name, about.getQuotaBytesUsed(), about.getQuotaBytesTotal());
	}

	public void downloadFile(String filename, String remotename) throws IOException {
		String fid = getFileID(remote_path + remotename);
		if (fid == null) {
			System.out.println("Download error: no such files!");
			return;
		}
		File gfile = service.files().get(fid).execute();
		HttpResponse resp = service.getRequestFactory()
				.buildGetRequest(new GenericUrl(gfile.getDownloadUrl()))
				.execute();
		InputStream sin = resp.getContent();
		java.io.File local = new java.io.File(filename);
		BufferedOutputStream fout = new BufferedOutputStream(
				new FileOutputStream(local));
		byte[] buf = new byte[65536];
		int len;
		while ((len = sin.read(buf)) > 0)
			fout.write(buf, 0, len);
		sin.close();
		fout.close();
	}

	public void uploadFile(String filename, String remotename)
			throws Exception {
		remotename = remote_path + remotename;
		String folder_id = getFolderID(remotename);
		String file_id = getFileID(remotename);

		ParentReference pr = new ParentReference();
		java.util.List<ParentReference> prlist = new ArrayList<ParentReference>();
		prlist.add(pr);
		pr.setId(folder_id);

		java.io.File fileContent = new java.io.File(filename);
		String mime_type = getMimeType(fileContent.getAbsolutePath());
		FileContent mediaContent = new FileContent(mime_type, fileContent);

		File body = new File();
		body.setTitle(extractName(remotename));
		body.setMimeType(mime_type);
		body.setParents(prlist);

		File res;
		if (file_id == null) {
			if (mediaContent.getLength() <= 0)
				res = service.files().insert(body).execute();
			else
				res = service.files().insert(body, mediaContent).execute();
			name2id.put(remotename, res.getId());
			id2name.put(res.getId(), remotename);
		} else {
			if (mediaContent.getLength() <= 0)
				res = service.files().update(file_id, body).execute();
			else
				res = service.files().update(file_id, body, mediaContent)
						.execute();
		}

		//System.out.println("FileID: " + res.getId());
	}

	public void createDir(String dirname) throws Exception {
		while (dirname.charAt(dirname.length() - 1) == '/')
			dirname = dirname.substring(0, dirname.length() - 1);
		dirname = remote_path + dirname;
		String file_id = getFileID(dirname);
		String folder_id = getFolderID(dirname);

		ParentReference pr = new ParentReference();
		java.util.List<ParentReference> prlist = new ArrayList<ParentReference>();
		prlist.add(pr);
		pr.setId(folder_id);

		File body = new File();
		body.setTitle(extractName(dirname));
		body.setMimeType("application/vnd.google-apps.folder");
		body.setParents(prlist);
		File res;
		if (file_id == null) {
			res = service.files().insert(body).execute();
			name2id.put(dirname, res.getId());
			id2name.put(res.getId(), dirname);
		} else {
			res = service.files().update(file_id, body).execute();
		}
		System.out.println("FileID: " + res.getId());
	}

	public void deleteFile(String filename) throws Exception {
		while (filename.charAt(filename.length() - 1) == '/')
			filename = filename.substring(0, filename.length() - 1);
		String file_id = getFileID(remote_path + filename);
		if (file_id != null)
			service.files().delete(file_id).execute();
	}

	public String getFileContent(String filename) throws Exception {
		String file_id = getFileID(remote_path + filename);
		String result = "";
		if (file_id != null) {
			File gfile = service.files().get(file_id).execute();
			HttpResponse resp = service.getRequestFactory()
					.buildGetRequest(new GenericUrl(gfile.getDownloadUrl()))
					.execute();
			InputStream sin = resp.getContent();

			byte[] buf = new byte[LIST_MAXSIZE];
			int len, off = 0;
			while ((len = sin.read(buf, off, LIST_MAXSIZE - off)) > 0)
				off += len;
			result = new String(buf, 0, off, Common.DEFAULT_CHARSET);
			sin.close();
		}
		return result;
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

	public void refreshList() throws IOException {
		if (root_id == null) {
			about = service.about().get().execute();
			root_id = about.getRootFolderId();
		}
		root_list.clear();
		name2id.clear();
		id2name.clear();
		name2id.put("", root_id);
		id2name.put(root_id, "");

		LinkedList<File> files = new LinkedList<File>();
		FileList new_list = service.files().list().setFields("items").execute();
		do {
			files.addAll(new_list.getItems());
			if (new_list.getNextPageToken() != null)
				new_list = service.files().list().setFields("items")
						.setPageToken(new_list.getNextPageToken()).execute();
		} while (new_list.getNextPageToken() != null);

		while (files.size() > 0) {
			LinkedList<File> reserve = new LinkedList<File>();
			for (File f : files) {
				String pid = f.getParents().get(0).getId();
				if (name2id.containsKey(extractFolder(remote_path)) && 
						name2id.get(extractFolder(remote_path)).equals(pid))
					root_list.add(f.getTitle());
				if (id2name.containsKey(pid)) {
					String cname = id2name.get(pid) + "/" + f.getTitle();
					name2id.put(cname, f.getId());
					id2name.put(f.getId(), cname);
				} else {
					reserve.add(f);
				}
			}
			if (files.size() == reserve.size())
				throw new IOException("File list error!");
			files = reserve;
		}
	}
}
