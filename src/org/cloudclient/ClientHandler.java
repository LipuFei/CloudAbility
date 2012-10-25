package org.cloudclient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientHandler implements Runnable {

	private String host;
	private int port;

	private String data;

	public ClientHandler(String host, int port, String data) {
		this.host = host;
		this.port = port;
		this.data = data;
	}

	@Override
	public void run() {
		/* create socket */
		try {
			Socket socket = new Socket(host, port);

			BufferedWriter writer =
				new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream()));

			writer.write(data);

			writer.close();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
