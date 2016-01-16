package SyncConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import MiniHttpServ.*;

import MultiSyncAPI.GDriveAPIs;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;

public class GDriveConfig {
	private static final String CRLF = "\r\n";
	public static final String CONFIG_URL = "http://file-sync.oicp.net:9000/";
	public static String REDIRECT_URI = CONFIG_URL + "oauth2callback";
	
	public static HttpTransport httpTransport = new NetHttpTransport();
	public static JsonFactory jsonFactory = new JacksonFactory();
	public static GoogleAuthorizationCodeFlow flow = null;
	
	static class GetToken extends HttpHandler {

		@Override
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) {
			try {
				flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
						jsonFactory, GDriveAPIs.CLIENT_ID, GDriveAPIs.CLIENT_SECRET,
						Arrays.asList(DriveScopes.DRIVE)).setAccessType("offline")
						.setApprovalPrompt("force").build();
				String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
				request.setStatusCode(302);
				request.setStatusMsg("Found");
				request.request_header.put("Location", url);
				return "".getBytes();
			} catch( Exception e ) {
				return "Error: failed to connect to google server".getBytes();
			}
		}
	}
	static class accessToken extends HttpHandler {
		public byte[] Get(String path, Map<String, String> params,
				HttpRequestHandler request) {
			if (flow == null) {
				return "You should login first".getBytes();
			}
			String html = "";
			try {
				String code = params.get("code");
				GoogleTokenResponse response = flow
						.newTokenRequest(code)
						.setRedirectUri(REDIRECT_URI).execute();
				GoogleCredential credential = new GoogleCredential.Builder()
						.setClientSecrets(GDriveAPIs.CLIENT_ID, GDriveAPIs.CLIENT_SECRET)
						.setJsonFactory(jsonFactory)
						.setTransport(httpTransport).build()
						.setFromTokenResponse(response);
				html = "<script language=\"javascript\">" + CRLF + 
						"window.opener.document.getElementsByName(\"gdrive_token1\")[0].value = \"" + 
						credential.getAccessToken() + "\";" +  CRLF +
						"window.opener.document.getElementsByName(\"gdrive_token2\")[0].value = \"" + 
						credential.getRefreshToken() + "\";" + CRLF +
						"window.close();" + CRLF +
						"</script>";
				flow = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return html.getBytes();
		}
	}
}
