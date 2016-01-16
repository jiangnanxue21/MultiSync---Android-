package MiniHttpServ;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;

import android.content.Context;

import com.example.multisync.ContextUtil;

public class HttpServUtils {
	public static byte[] getResourceContent(String filename) {
		byte[] buf = new byte[4096];
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			InputStream is = HttpServUtils.class.getResourceAsStream(filename);
			BufferedInputStream bi = new BufferedInputStream(is);
			int len;
			while ((len = bi.read(buf)) != -1)
				bos.write(buf, 0, len);
			bi.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("error: failed to open file: " + filename);
			return null;
		}
		
		return bos.toByteArray();
	}
	
	public static byte[] getResourceContent(int resid) {
		byte[] buf = new byte[4096];
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			Context c = ContextUtil.getInstance();
			InputStream is = c.getResources().openRawResource(resid);;
			BufferedInputStream bi = new BufferedInputStream(is);
			int len;
			while ((len = bi.read(buf)) != -1)
				bos.write(buf, 0, len);
			bi.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("error: failed to open file: " + resid);
			return null;
		}
		
		return bos.toByteArray();
	}
	
	public static byte[] getFileContent(String filename) {
		byte[] buf = new byte[4096];
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			InputStream is = new FileInputStream(new File(filename));
			BufferedInputStream bi = new BufferedInputStream(is);
			int len;
			while ((len = bi.read(buf)) != -1)
				bos.write(buf, 0, len);
			bi.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("error: failed to open file: " + filename);
			return null;
		}
		
		return bos.toByteArray();
	}
	
	public static String getMimeType(String fullpath)
			throws java.io.IOException {
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String type = fileNameMap.getContentTypeFor("file://" + fullpath);

		return type;
	}
}
