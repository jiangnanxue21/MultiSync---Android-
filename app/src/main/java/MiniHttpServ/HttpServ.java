/*
 * Author:	周浩程
 * Date:	2013-1-26
 * E-Mail:	zhc105@163.com
 */

package MiniHttpServ;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import MiniHttpServ.HttpHandler;

public class HttpServ {
	public ArrayList<HandlerEntry> Handlers = new ArrayList<HandlerEntry>();
	private TcpServer request;
	private ServerSocket socket;

	public void start(String Host, int Port) throws IOException, UnknownHostException {
		InetAddress inet_addr = InetAddress.getByName(Host);
		socket = new ServerSocket(Port, 5, inet_addr);
		request = new TcpServer(this, socket);
		request.start();
	}
	
	public void stop(Runnable callback) {
		request.stop = true;
		request.stoppedCallback = callback;
		try {
			socket.close();
		} catch (IOException e) {}
	}
	
	public void stop() {
		stop(null);
	}

	public void addPages(String path, HttpHandler handler) {
		Handlers.add(new HandlerEntry(path, handler));
	}
}

class TcpServer extends Thread {
	private HttpServ serv_class;
	private ServerSocket serv_socket;
	public boolean stop = false;
	public Runnable stoppedCallback = null;

	public TcpServer(HttpServ serv_class, ServerSocket socket) {
		this.serv_socket = socket;
		this.serv_class = serv_class;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("httpServer started on http:/" 
				+ serv_socket.getInetAddress().toString() + ":"
				+ serv_socket.getLocalPort());

		while (!stop) {
			try {
				Socket socket = serv_socket.accept();
				socket.setSoTimeout(10000);
				HttpRequestHandler request = new HttpRequestHandler(serv_class, socket);
				Thread thread = new Thread(request);
				thread.start();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (stoppedCallback != null)
			stoppedCallback.run();
	}
}

class HandlerEntry {
	public String page_name;
	public HttpHandler handler;
	
	public HandlerEntry(String name, HttpHandler handler) {
		this.page_name = name;
		this.handler = handler;
	}
}