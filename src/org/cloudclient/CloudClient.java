package org.cloudclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class CloudClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * This is a simple client for testing.
		 * every time you press ENTER, it will generate a job, connect to the
		 * server, and submit this job.
		 * 
		 * Press CTRL+C to exit this program.
		 */
		System.out.println("every time you press ENTER, it will generate a job, connect to the server, and submit this job.");
		System.out.println("Press CTRL+C to exit this program.");
		try {
			int count = 0;
			while (true) {
				System.in.read();

				String data = createJobSubmitData(count++);

				ClientHandler handler = new ClientHandler("localhost", 36023, data);
				Thread thread = new Thread(handler);
				thread.start();

				thread.join();

				String msg = String.format("%d jobs have been submitted.", count);
				System.out.println(msg);
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String createJobSubmitData(int id) {
		/*
		 * Note: change whatever you need to make it suitable for your situation.
		 */
		String data = "";
		/* remote dir */
		data += String.format("%s:%s;", "REMOTE_DIR", "/opt");
		/* the application tarball location on the local machine */
		data += String.format("%s:%s;", "APP.LOCAL", "/home/lfei/in4392/largelab/ffmpeg-centos.tar.gz");
		/* the application path on the remote machine */
		data += String.format("%s:%s;", "APP.REMOTE", "ffmpeg/bin/ffmpeg");
		/*
		 * the input file path and the output file path
		 * the output file is automatically assigned a name "output#.mov",
		 * so that it won't conflict with other output files when you are
		 * downloading them to the local directory.
		 */
		String param = String.format("-i /opt/cloudatlas-trailer1b_h1080p.mov -target ntsc-dvd -y /opt/output%d.mov", id);
		data += String.format("%s:%s;", "APP.PARAMS", param);
		/* the input file path on the local machine */
		data += String.format("%s:%s;", "INPUT.LOCAL", "/home/lfei/in4392/largelab/cloudatlas-trailer1b_h1080p.mov");
		/* the input file path on the remote machine, with respect to REMOTE_DIR */
		data += String.format("%s:%s;", "INPUT.REMOTE", "cloudatlas-trailer1b_h1080p.mov");
		/* the directory on the local machine that you want to store the output file downloaded from the VM */
		data += String.format("%s:%s;", "OUTPUT.LOCAL", "/home/lfei/in4392/largelab");
		/* the output file on the remote machine */
		String outfile = String.format("output%d.mov", id);
		data += String.format("%s:%s;", "OUTPUT.REMOTE", outfile);

		return data;
	}

}
