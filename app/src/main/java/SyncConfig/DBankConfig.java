package SyncConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.baidu.api.utils.HttpUtil;
import com.baidu.api.utils.JsonUtil;
import com.dropbox.client2.exception.DropboxException;
import com.kuaipan.client.OauthUtility;
import com.kuaipan.client.model.Consumer;
import com.kuaipan.client.model.KuaipanURL;
import com.kuaipan.client.model.RequestToken;
import com.kuaipan.client.model.TokenPair;

import MiniHttpServ.HttpHandler;
import MiniHttpServ.HttpRequestHandler;
import MultiSyncAPI.DBankAPIs;
import SyncMain.Common;

public class DBankConfig {
	private static final String CRLF = "\r\n";
	private static final String HOST = "login.dbank.com";
	public static final String CALLBACK = "/dbank_token";
	
	private static Consumer consumer = null;
	private static TokenPair token = null;

	static class authorize extends HttpHandler {
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) throws IOException  {
		
			consumer = new Consumer(DBankAPIs.CONSUMER_KEY, DBankAPIs.CONSUMER_SECRET);
			
			HashMap<String, String> auth_params = new HashMap<String, String>();
			auth_params.put("oauth_callback", Common.CONFIG_HOST + CALLBACK);
			
			KuaipanURL url = OauthUtility.buildURL("GET", HOST,
					"/oauth1/request_token", auth_params, consumer, null, 32, false);
			url.convert2Get();
			
			String resp = HttpUtil.doGet(url.url, null);
			JSONObject jsonobj = JsonUtil.parseJson(resp);
			String request_token = jsonobj.get("oauth_token").toString();
			String request_secret = jsonobj.get("oauth_token_secret").toString();
			token = new RequestToken(request_token, request_secret);
			String AuthorURL = "http://login.dbank.com/oauth1/authorize?oauth_token="
					+ request_token;
			
			
			request.setStatusCode(302);
			request.setStatusMsg("Found");
			request.request_header.put("Location", AuthorURL);
			return "".getBytes();
		}
	}
		
	static class accessToken extends HttpHandler {
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) throws DropboxException, IOException {
			if (consumer == null || token == null) {
				return "You should login first".getBytes();
			}
				
			String oauth_verifier = params.get("oauth_verifier");
			
			HashMap<String, String> token_params = new HashMap<String, String>();
			token_params.put("oauth_verifier", oauth_verifier);
			KuaipanURL url = OauthUtility.buildURL("GET", HOST, "/oauth1/access_token", token_params,
					consumer, token, 32, false);
			url.convert2Get();
			
			String resp = HttpUtil.doGet(url.url, null);
			JSONObject jsonobj = JsonUtil.parseJson(resp);
			String oauth_token = jsonobj.get("oauth_token").toString();
			String oauth_secret = jsonobj.get("oauth_token_secret").toString();
				
			String html = "<html><body>acquire token successful! please close this window..." + CRLF +
					"<script language=\"javascript\">" + CRLF + 
					"opener = window.opener;" + CRLF +
					"opener.document.getElementsByName(\"dbank_token1\")[0].value = \"" + 
					oauth_token + "\";" +  CRLF +
					"opener.document.getElementsByName(\"dbank_token2\")[0].value = \"" + 
					oauth_secret + "\";" + CRLF +
					"window.close();" + CRLF +
					"</script></body></html>";
				
			consumer = null;
			token = null;
			return html.getBytes();
		}
	}
}
