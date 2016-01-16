package SyncConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.baidu.api.utils.HttpUtil;
import com.baidu.api.utils.JsonUtil;

import MiniHttpServ.*;
import MultiSyncAPI.BoxAPIs;

public class BoxConfig extends HttpHandler {
	private static final String CRLF = "\r\n";
	public static final String CALLBACK = "/box_token";
	
	public byte[] Get(String path, Map<String, String> params,
			HttpRequestHandler request) throws IOException  {
		
		if (request.host != null && !request.host.equals("file-sync.oicp.net:9000")) {
			request.setStatusCode(302);
			request.setStatusMsg("Found");
			request.request_header.put("Location", "http://file-sync.oicp.net:9000" + CALLBACK + "?code=" + params.get("code"));
			return "".getBytes();
		}
		
		String url = "https://www.box.com/api/oauth2/token";
		
		HashMap<String, String> post_params = new HashMap<String, String>();
		post_params.put("grant_type", "authorization_code");
		post_params.put("code", params.get("code"));
		post_params.put("client_id", BoxAPIs.CLIENT_ID);
		post_params.put("client_secret", BoxAPIs.CLIENT_SECRET);
		
		String result = HttpUtil.doPost(url, post_params);
		
		JSONObject jsonobj = JsonUtil.parseJson(result);
		
		String html = "<script language=\"javascript\">" + CRLF + 
				"window.opener.document.getElementsByName(\"box_token1\")[0].value = \"" + 
				jsonobj.get("access_token").toString() + "\";" +  CRLF +
				"window.opener.document.getElementsByName(\"box_token2\")[0].value = \"" + 
				jsonobj.get("refresh_token").toString() + "\";" + CRLF +
				"window.close();" + CRLF +
				"</script>";
			
		return html.getBytes();
	}

}
