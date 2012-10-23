/**
 * 
 */
package org.cloudability;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;

import org.cloudability.resource.ResourceManager;
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
		try {
			DataManager.initialize("config/cloud.config");
			ResourceManager.initialize();

		} catch (CloudConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		testAll();
		//testSCP();
		//testConfig();
		//testScheduler();

		/* finalize resource manager */
		ResourceManager.instance().finalize();
	}

	public static void testSCP() {
		//SSHHandler.getSession("");
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

	public static void testConfig() {
		HashMap<String, String> configMap =
				DataManager.instance().getConfigMap();

		String serverUrl = configMap.get("ONE.XMLRPC_URL");
		String username = configMap.get("ONE.USERNAME");
		String password = configMap.get("ONE.PASSW0RD");
		String vmTemplate = configMap.get("ONE.VM_TEMPLATE");

		System.out.println("server url: " + serverUrl);
		System.out.println("username: " + username);
		System.out.println("password: " + password);
		System.out.println("vm template: " + vmTemplate);
	}

	public static void testScheduler() {
		Job[] jobs = new Job[5];
		for (int i = 0; i < 5; i++) {
			jobs[i] = new Job(i);
			DataManager.instance().getPendingJobQueue().addJob(jobs[i]);
		}

		Scheduler scheduler = new Scheduler();
		Thread thread = new Thread(scheduler);
		thread.start();

		try {
			while (true) {
				System.in.read();
				scheduler.setToStop();
				thread.join();
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
