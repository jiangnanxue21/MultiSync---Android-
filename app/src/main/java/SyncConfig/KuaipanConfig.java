package SyncConfig;

import java.util.Map;

import com.kuaipan.client.KuaipanAPI;
import com.kuaipan.client.session.OauthSession;

import MiniHttpServ.HttpHandler;
import MiniHttpServ.HttpRequestHandler;

import MultiSyncAPI.KuaipanAPIs;

public class KuaipanConfig {
	private static final String CRLF = "\r\n";
	private static final String CALLBACK = "http://file-sync.oicp.net:9000/kuaipan_token";
	private static KuaipanAPI api = null;
	private static OauthSession session = null;
	
	public static class requestToken extends HttpHandler {
		@Override
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) throws Exception {
			session = new OauthSession(KuaipanAPIs.CONSUMER_KEY, KuaipanAPIs.CONSUMER_SECRET, 
					OauthSession.Root.APP_FOLDER);
			api = new KuaipanAPI(session);
			String auth_url = api.requestToken(CALLBACK);
			request.setStatusCode(302);
			request.setStatusMsg("Found");
			request.request_header.put("Location", auth_url);
			return "".getBytes();
		}
	}
	
	public static class accessToken extends HttpHandler {
		@Override
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) throws Exception {
			if (api == null || session == null) {
				return "You should login first".getBytes();
			}
			api.accessToken();
			System.out.println(session.token);
			String html = "<script language=\"javascript\">" + CRLF + 
					"window.opener.document.getElementsByName(\"kuaipan_token1\")[0].value = \"" + 
					session.token.key + "\";" +  CRLF +
					"window.opener.document.getElementsByName(\"kuaipan_token2\")[0].value = \"" + 
					session.token.secret + "\";" + CRLF +
					"window.close();" + CRLF +
					"</script>";
			api = null;
			session = null;
			return html.getBytes();
		}
	}
}


