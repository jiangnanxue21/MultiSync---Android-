package SyncConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.baidu.api.utils.HttpUtil;
import com.baidu.api.utils.JsonUtil;

import MiniHttpServ.*;
import MultiSyncAPI.BaiduAPIs;
import SyncMain.Common;

public class BaiduConfig extends HttpHandler {
	private static final String CRLF = "\r\n";
	public static final String CALLBACK = "/baidu_token";
	
	public byte[] Get(String path, Map<String, String> params,
			HttpRequestHandler request) throws IOException  {
		
		String url = "https://openapi.baidu.com/oauth/2.0/token";
		
		HashMap<String, String> get_params = new HashMap<String, String>();
		get_params.put("grant_type", "authorization_code");
		get_params.put("code", params.get("code"));
		get_params.put("client_id", BaiduAPIs.API_KEY);
		get_params.put("client_secret", BaiduAPIs.SECRET_KEY);
		get_params.put("redirect_uri", Common.CONFIG_HOST + CALLBACK);
		
		String result = HttpUtil.doGet(url, get_params);
		
		JSONObject jsonobj = JsonUtil.parseJson(result);
		
		String html = "<script language=\"javascript\">" + CRLF + 
				"window.opener.document.getElementsByName(\"baidu_token1\")[0].value = \"" + 
				jsonobj.get("access_token").toString() + "\";" +  CRLF +
				"window.opener.document.getElementsByName(\"baidu_token2\")[0].value = \"" + 
				jsonobj.get("refresh_token").toString() + "\";" + CRLF +
				"window.close();" + CRLF +
				"</script>";
			
		return html.getBytes();
	}

}
