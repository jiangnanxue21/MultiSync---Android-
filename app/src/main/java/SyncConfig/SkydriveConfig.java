package SyncConfig;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.baidu.api.utils.HttpUtil;
import com.baidu.api.utils.JsonUtil;

import MiniHttpServ.HttpHandler;
import MiniHttpServ.HttpRequestHandler;
import MultiSyncAPI.SkydriveAPIs;

public class SkydriveConfig extends HttpHandler {
	private static final String CRLF = "\r\n";
	
	@Override
	public byte[] Get(String path, Map<String, String> params,
			HttpRequestHandler request) {
		String code = params.get("code");
		String url = "https://login.live.com/oauth20_token.srf";

		try {
			HashMap<String, String> postparam = new HashMap<String, String>();
			postparam.put("client_id", SkydriveAPIs.CLIENT_ID);
			postparam.put("client_secret", SkydriveAPIs.CLIENT_SECRET);
			postparam.put("redirect_uri", "http://file-sync.oicp.net:9000/code");
			postparam.put("code", code);
			postparam.put("grant_type", "authorization_code");
			String result = HttpUtil.doPost(url, postparam);
			JSONObject jsonobj = JsonUtil.parseJson(result);
			String token = jsonobj.get("access_token").toString();
			String refresh = jsonobj.get("refresh_token").toString();
			System.out.println("expires: " + jsonobj.get("expires_in"));
			String html = "<script language=\"javascript\">" + CRLF + 
					"window.opener.document.getElementsByName(\"skydrive_token1\")[0].value = \"" + token + "\";" +  CRLF +
					"window.opener.document.getElementsByName(\"skydrive_token2\")[0].value = \"" + refresh + "\";" + CRLF +
					"window.close();" + CRLF +
					"</script>";
			return html.getBytes();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
