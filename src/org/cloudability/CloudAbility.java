/**
 * 
 */
package org.cloudability;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;

import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3FileHandle;

import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMInstance;
import org.cloudability.scheduling.Job;
import org.cloudability.scheduling.Scheduler;
import org.cloudability.server.ClientRequestListener;
import org.cloudability.util.CloudConfigException;

import org.koala.internals.SSHException;
import org.koala.internals.SSHHandler;

/**
 * The main class of CloudAbility
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class CloudAbility {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();

		/* common initialization */
		//try {
			//DataManager.initialize("config/cloud.config");
			//ResourceManager.initialize();

		//} catch (CloudConfigException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}

		//testAll();
		//testSCP();
		testJob();

		/* finalize resource manager */
		//ResourceManager.instance().finalize();
	}

	public static void testJob() {
		try {
			DataManager.initialize("config/cloud.config");
			VMInstance vm = new VMInstance(1);
			vm.setIpAddress("10.141.3.134");

			HashMap<String, String> p = new HashMap<String, String>();
			p.put("REMOTE_DIR", "/opt");
			p.put("APP.LOCAL", "/home/lfei/in4392/largelab/ffmpeg-centos.tar.gz");
			p.put("APP.REMOTE", "ffmpeg/bin/ffmpeg");
			p.put("APP.PARAMS", "-i /opt/cloudatlas-trailer1b_h1080p.mov -target ntsc-dvd -y /opt/output.mov");
			p.put("INPUT.LOCAL", "/home/lfei/in4392/largelab/cloudatlas-trailer1b_h1080p.mov");
			p.put("INPUT.REMOTE", "cloudatlas-trailer1b_h1080p.mov");
			p.put("OUTPUT.LOCAL", "/home/lfei/in4392/largelab");
			p.put("OUTPUT.REMOTE", "output.mov");
			Job job = new Job(1, p);
			job.setVMInstance(vm);

			Thread t = new Thread(job);
			t.start();

			t.join();
			
		} catch (CloudConfigException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void testSCP() {
		try {
			SFTPv3Client sftpClient =
					SSHHandler.getSftpClient("10.141.3.134", "root");

			/* upload a file */
			SFTPv3FileHandle fileHandle =
					sftpClient.createFile("/opt/test.dat");

			String data = "This is a test.\n";

			sftpClient.write(fileHandle, 0, data.getBytes("UTF-8"), 0,
					data.getBytes("UTF-8").length);

			/* close */
			sftpClient.closeFile(fileHandle);
			sftpClient.close();

		} catch (SSHException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void testAll() {
		/* start scheduler */
		Scheduler scheduler = new Scheduler();
		Thread schedulerThread = new Thread(scheduler);
		schedulerThread.start();

		/* start request listener */
		int port = Integer.parseInt(
			DataManager.instance().getConfigMap().get("CONFIG.LISTEN_PORT")
			);
		ClientRequestListener listener = new ClientRequestListener(port);
		Thread listenThread = new Thread(listener);
		listenThread.start();

		try {
			while (true) {
				System.in.read();
				/* stop listener */
				listener.stopListening();
				listenThread.join();

				/* stop scheduler */
				scheduler.setToStop();
				schedulerThread.join();
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
