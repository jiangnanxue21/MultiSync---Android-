package SyncConfig;

import java.util.Map;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;

import MultiSyncAPI.DropboxAPIs;
import MiniHttpServ.*;

public class DropboxConfig {
	private static final String CRLF = "\r\n";
	private static final String CALLBACK = "http://file-sync.oicp.net:9000/dropbox_token";
	private static WebAuthSession session = null;
	private static WebAuthInfo result = null;
	
	static class authorize extends HttpHandler {
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) throws DropboxException {
			AppKeyPair appKeys = new AppKeyPair(DropboxAPIs.APP_KEY, DropboxAPIs.APP_SECRET);
			session = new WebAuthSession(appKeys, DropboxAPIs.ACCESS_TYPE);
			DropboxAPI<WebAuthSession> mDBApi = new DropboxAPI<WebAuthSession>(session);
			result = mDBApi.getSession().getAuthInfo(CALLBACK);
			request.setStatusCode(302);
			request.setStatusMsg("Found");
			request.request_header.put("Location", result.url);
			return "".getBytes();
		}
	}
	
	static class accessToken extends HttpHandler {
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) throws DropboxException {
			if (session == null || result == null) {
				return "You should login first".getBytes();
			}
			
			System.out.println("Connected UID: " + session.retrieveWebAccessToken(result.requestTokenPair));
			String html = "<script language=\"javascript\">" + CRLF + 
					"window.opener.document.getElementsByName(\"dropbox_token1\")[0].value = \"" + 
					session.getAccessTokenPair().key + "\";" +  CRLF +
					"window.opener.document.getElementsByName(\"dropbox_token2\")[0].value = \"" + 
					session.getAccessTokenPair().secret + "\";" + CRLF +
					"window.close();" + CRLF +
					"</script>";
			
			session = null;
			result = null;
			return html.getBytes();
		}
	}
}
