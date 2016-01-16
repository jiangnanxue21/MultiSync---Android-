/*
 * Author:	周浩程
 * Date:	2013-3-1
 * E-Mail:	zhc105@163.com
 */

package SyncConfig;

import com.example.multisync.R;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import MiniHttpServ.HttpHandler;
import MiniHttpServ.HttpRequestHandler;
import MiniHttpServ.HttpServ;
import MiniHttpServ.HttpServUtils;
import SyncMain.Common;

public class SyncSetting {
	public static HttpServ httpserv;
	
	public static void main() throws Exception {
		httpserv = new HttpServ();
		httpserv.addPages("/index.html", new ConfigPage());
		httpserv.addPages("/save_setting", new SaveConfig());
		httpserv.addPages("/code", new SkydriveConfig());
		httpserv.addPages("/kuaipan", new KuaipanConfig.requestToken());
		httpserv.addPages("/kuaipan_token", new KuaipanConfig.accessToken());
		httpserv.addPages("/gdrive", new GDriveConfig.GetToken());
		httpserv.addPages("/oauth2callback", new GDriveConfig.accessToken());
		httpserv.addPages("/dropbox", new DropboxConfig.authorize());
		httpserv.addPages("/dropbox_token", new DropboxConfig.accessToken());
		httpserv.addPages("/dbank", new DBankConfig.authorize());
		httpserv.addPages(BaiduConfig.CALLBACK, new BaiduConfig());
		httpserv.addPages(BoxConfig.CALLBACK, new BoxConfig());
		httpserv.start("0.0.0.0", 9000);
	}
}

class SaveConfig extends HttpHandler {
	@Override
	public byte[] Post(
			String path, 
			Map<String, String> params, 
			Map<String, String> post_params, 
			HttpRequestHandler request) {
		// TODO Auto-generated method stub
		try {
			TreeMap<String, String> sorted = new TreeMap<String, String>(post_params);
			Common.putConfig(sorted);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return new String( "<html><body>Error:" + e.getMessage() + "</body></html>").getBytes();
		}

		return new String("<html>"
				+ "<head><title>Congratulations!</title></head>"
				+ "<body>Your settings has successfully been saved.</body>"
				+ "</html>").getBytes();
	}
}

class ConfigPage extends HttpHandler {

	@Override
	public byte[] Get(String path, Map<String, String> params,
			HttpRequestHandler request) {
		// TODO Auto-generated method stub
		return HttpServUtils.getResourceContent(R.raw.index);
	}
}
