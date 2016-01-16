package MiniHttpServ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

public class HttpRequestHandler implements Runnable {
	final static String CRLF = "\r\n";
	final static String DEFAULT_PAGE = "index.html";
	final static String SERVER_INFO = "Mini Java HTTP Server";
	final static String DEFAULT_CHARSET = "UTF-8";
	HttpServ serv_class;
	Socket socket;
	InputStream sin;
	OutputStream sout;
	BufferedReader br;
	int Cont_Length;
	
	public String host;
	public HashMap<String, String> request_header;

	public HttpRequestHandler(HttpServ serv_class, Socket socket) throws IOException {
		this.socket = socket;
		this.serv_class = serv_class;
		sin = socket.getInputStream();
		sout = socket.getOutputStream();
		br = new BufferedReader(new InputStreamReader(sin));
		Cont_Length = -1;
	}

	@SuppressWarnings("unused")
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			System.out.println("New connection accepted "
					+ socket.getInetAddress() + ":" + socket.getPort());
			String line = br.readLine();
			StringTokenizer s = new StringTokenizer(line);

			String method = "", path, version;
			HashMap<String, String> param_table = new HashMap<String, String>();
			String paramstr, status, server, contentType, contentLength;
			method = s.nextToken().toUpperCase();
			path = s.nextToken();
			version = s.nextToken();
			int content_length = 0;
			int parampos = path.indexOf('?');
			
			if (method.equals("GET") || method.equals("POST")) {
				if (parampos != -1) {
					paramstr = path.substring(parampos + 1);
					String[] params = paramstr.split("&");
					for (int i = 0; i < params.length; i++) {
						int pos = params[i].indexOf('=');
						if (pos != -1) {
							String key = params[i].substring(0, pos);
							String value;
							if (pos >= params[i].length() - 1) {
								value = "";
							} else {
								value = params[i].substring(pos + 1);
							}
							value = URLDecoder.decode(value, DEFAULT_CHARSET);
							param_table.put(key, value);
						}
					}
					path = path.substring(0, parampos);
				}

				if (path.charAt(path.length() - 1) == '/')
					path += DEFAULT_PAGE;

				line = br.readLine();
				
				while (line.length() > 0) { // Read HTTP Header
					int pos;
					if ((pos = line.indexOf(":")) >= 0) {
						String attrib_name = line.substring(0, pos).trim().toUpperCase();
						String attrib_val = line.substring(pos + 1);
						if (attrib_name.equals("CONTENT-LENGTH")) {
							content_length = Integer.parseInt(attrib_val.trim());
						} else if (attrib_name.equals("HOST")) {
							host = attrib_val.trim();
						}
					}
					line = br.readLine();
				}

				byte[] buffer = null;
				request_header = new HashMap<String, String>();
				setStatusCode(200);
				setStatusMsg("OK");
				setContentType("text/html");
				
				for (HandlerEntry entry : serv_class.Handlers) { 
					String key = entry.page_name;
					int len = key.length() - 1;
					if (key.charAt(len) == '*' && len <= path.length()) {
						if (key.substring(0, len).equals(path.substring(0, len))) {
							if (method.equals("GET"))
								buffer = entry.handler.Get(path, param_table, this);
							else if (method.equals("POST"))
								buffer = entry.handler.Post(path, param_table, 
										getParams(br, content_length), this);
						}	
					}
					else if (key.equals(path)) 
						if (method.equals("GET"))
							buffer = entry.handler.Get(path, param_table, this);
						else if (method.equals("POST"))
							buffer = entry.handler.Post(path, param_table, 
									getParams(br, content_length), this);
					if (buffer != null)
						break;
				}

				server = "Server: " + SERVER_INFO + CRLF;

				if (buffer == null) {
					status = "HTTP/1.0 404 Not Found" + CRLF;
					contentType = "Content-type: text/html" + CRLF;
					buffer = new String("<HTML>"
							+ "<HEAD><TITLE>404 Not Found</TITLE></HEAD>"
							+ "<BODY>404 Not Found" + "<br></BODY></HTML>")
							.getBytes();
					contentLength = "Content-Length:" + String.valueOf(buffer.length) + CRLF;
				} else {
					status = "HTTP/1.0 " + request_header.get("status_code") + " " 
							+ request_header.get("status_msg") + CRLF;
					contentType = "Content-type: " + request_header.get("content-type")
							+ CRLF;
					contentLength = "Content-Length: " + String.valueOf(buffer.length) + CRLF;
				}
				request_header.remove("status_code");
				request_header.remove("status_msg");
				request_header.remove("content-type");
				sout.write(status.getBytes());
				sout.write(server.getBytes());
				sout.write(contentType.getBytes());
				sout.write(contentLength.getBytes());
				for (Entry<String, String> p : request_header.entrySet()) {
					sout.write((p.getKey() + ": " + p.getValue() + CRLF).getBytes());
				}
				sout.write(CRLF.getBytes());
				sout.write(buffer);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(socket.getInetAddress() + ":" + socket.getPort() + " error: " + e.getMessage());
		}
		try {
			
			br.close();
			sin.close();
			sout.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, String> getParams(BufferedReader br, int clen) throws IOException {
		if (clen < 0 || clen > 0x6400000) 
			return null;
		Map<String, String> param_table = new HashMap<String, String>();
		
		
		char[] buf = new char[4096];
		String str = "";
		int now = 0, len;
		while (clen > 0) {
			len = br.read(buf, now, 4096);
			
			String tmp = new String(buf, 0, len);
			clen -= tmp.getBytes().length;
			str += tmp;
		}
		
		
		String[] params = str.split("&");
		for (int i = 0; i < params.length; i++) {
			String tmp[] = params[i].split("=");
			if (tmp.length > 0) {
				if (tmp.length >= 2) {
					tmp[1] = URLDecoder.decode(tmp[1], DEFAULT_CHARSET);
					param_table.put(tmp[0], tmp[1]);
				}
				else
					param_table.put(tmp[0], "");
			}
		}
		
		return param_table;
	}
	
	public void setStatusCode(int code) {
		request_header.put("status_code", String.valueOf(code));
	}
	
	public void setStatusMsg(String status) {
		request_header.put("status_msg", status);
	}
	
	public void setContentType(String content_type) {
		request_header.put("content-type", content_type);
	}

}