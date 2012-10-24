/**
 * 
 */
package org.cloudability;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;

import org.cloudability.broker.CloudBroker;
import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMInstance;
import org.cloudability.scheduling.Job;
import org.cloudability.scheduling.Scheduler;
import org.cloudability.server.ClientRequestListener;
import org.cloudability.util.BrokerException;
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
	 * @throws BrokerException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws BrokerException, InterruptedException {
		BasicConfigurator.configure();

		/* common initialization */
		try {
			DataManager.initialize("config/cloud.config");
			ResourceManager.initialize();

		} catch (CloudConfigException e) {
			e.printStackTrace();
		}

		//testAll();
		//testJob();
		//testVM();
		//testJobWithVM();
		testAuto();

		/* finalize resource manager */
		ResourceManager.instance().finalize();
	}

	public static void testAuto() {
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
			int i = 0;
			while (true) {
				System.in.read();

				/* at most 5 jobs */
				if (i++ < 5) {
					HashMap<String, String> p = new HashMap<String, String>();
					p.put("REMOTE_DIR", "/opt");
					p.put("APP.LOCAL", "/home/lfei/in4392/largelab/ffmpeg-centos.tar.gz");
					p.put("APP.REMOTE", "ffmpeg/bin/ffmpeg");
					p.put("APP.PARAMS", String.format("-i /opt/cloudatlas-trailer1b_h1080p.mov -target ntsc-dvd -y /opt/output%d.mov", i));
					p.put("INPUT.LOCAL", "/home/lfei/in4392/largelab/cloudatlas-trailer1b_h1080p.mov");
					p.put("INPUT.REMOTE", "cloudatlas-trailer1b_h1080p.mov");
					p.put("OUTPUT.LOCAL", "/home/lfei/in4392/largelab");
					p.put("OUTPUT.REMOTE", String.format("output%d.mov", i));
					Job job = new Job(i, p);
					DataManager.instance().getPendingJobQueue().addJob(job);
					continue;
				}

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

	public static void testJobWithVM() throws BrokerException, InterruptedException {
		CloudBroker broker = CloudBroker.createBroker("ONE");

		System.out.println("Allocating VM...");
		VMInstance vm = broker.allocateVM();

		System.out.println("Preparing Job...");
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

		System.out.println("Starting Job...");
		Thread t = new Thread(job);
		t.start();

		System.out.println("Waiting for the Job to finish...");
		t.join();

		System.out.println("Finalizing VM...");
		broker.finalizeVM(vm);
	}

	public static void testVM() throws BrokerException {
		CloudBroker broker = CloudBroker.createBroker("ONE");

		System.out.println("Allocating VM.");
		VMInstance vm = broker.allocateVM();

		System.out.println("Finalizing VM.");
		broker.finalizeVM(vm);
	}

	public static void testJob() {
		try {
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

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
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
