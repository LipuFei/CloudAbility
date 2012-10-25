/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import org.apache.log4j.Logger;

import com.trilead.ssh2.Session;

import org.cloudability.DataManager;
import org.cloudability.broker.CloudBroker;
import org.cloudability.resource.VMInstance.VMStatus;
import org.cloudability.util.BrokerException;
import org.koala.internals.SSHException;
import org.koala.internals.SSHHandler;

/**
 * This runnable class is responsible for allocating and monitoring a VM
 * instance to make sure that it is ready before it is added into the resource
 * list.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class VMAgent extends Thread {

	/* default timeout is 2 minutes */
	private final static long defaultTimeout = 2 * 60 * 1000;
	/* default wait period is 300 milliseconds */
	private final static long defaultWaitPeriod = 300;

	private Logger logger = Logger.getLogger(VMAgent.class);

	private volatile boolean toStop;

	/**
	 * Constructor.
	 * @param vm The VM instance to monitor.
	 */
	public VMAgent() {
		super();
		this.toStop = false;
	}

	/**
	 * Sets the stop signal in order to stop it gracefully.
	 */
	public void setToStop() {
		this.toStop = true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String msg = "";
		long startTime = System.currentTimeMillis();

		VMInstance vm = null;
		try {
			/* allocate a VM instance */
			CloudBroker borker = CloudBroker.createBroker("ONE");
			vm = borker.allocateVM();

			/* wait for the VM instance to be RUNNING */
			msg = String.format("Agent is waiting for VM#%d to be RUNNING...", vm.getId());
			logger.debug(msg);
			/* check status first */
			if (vm.getStatus() == VMStatus.SHUTDOWN || vm.getStatus() == VMStatus.UNKNOWN) {
				msg = String.format("VM#%d status is invalid.", vm.getId());
				throw new Exception(msg);
			}
			/* wait until it it running */
			CloudBroker broker = CloudBroker.createBroker("ONE");
			while (vm.getStatus() != VMStatus.RUNNING) {
				broker.updateInfo(vm);

				/* check stop signal */
				if (toStop) {
					msg = String.format("Agent for VM#%d is stopping.", vm.getId());
					throw new Exception(msg);
				}

				/* check time out */
				if (System.currentTimeMillis() - startTime > defaultTimeout) {
					msg = String.format("VM#%d preparation timeout.", vm.getId());
					throw new Exception(msg);
				}

				Thread.sleep(defaultWaitPeriod);
			}

			/* use ping to check if system has been initialized */
			msg = String.format("Pinging VM#%d...", vm.getId());
			logger.debug(msg);
			while (true) {
				String cmd = String.format("ping -c 1 %s", vm.getIpAddress());
				Process process = Runtime.getRuntime().exec(cmd);
				boolean reachable = (process.waitFor() == 0);

				if (reachable) break;

				/* check stop signal */
				if (toStop) {
					msg = String.format("Agent for VM#%d is stopping.", vm.getId());
					throw new Exception(msg);
				}

				/* check time out */
				if (System.currentTimeMillis() - startTime > defaultTimeout) {
					msg = String.format("VM#%d preparation timeout.", vm.getId());
					throw new Exception(msg);
				}

				Thread.sleep(defaultWaitPeriod);
			}

			/* use SSH to check if reachable */
			msg = String.format("Trying to reach VM#%d using SSH.", vm.getId());
			logger.debug(msg);
			String vmUsername =
					DataManager.instance().getConfigMap().get("VM.USERNAME");
			boolean sshTest = false;
			while (!sshTest) {
				try {
					Session session = SSHHandler.getSession(vm.getIpAddress(), vmUsername);
					session.close();
					sshTest = true;
				} catch (SSHException e) {
					sshTest = false;
				}

				/* check stop signal */
				if (toStop) {
					msg = String.format("Agent for VM#%d is stopping.", vm.getId());
					throw new Exception(msg);
				}

				/* check timeout */
				if (System.currentTimeMillis() - startTime > defaultTimeout) {
					msg = String.format("VM#%d preparation timeout.", vm.getId());
					throw new Exception(msg);
				}

				Thread.sleep(defaultWaitPeriod);
			}

			/* VM instance is ready, put it into the resource list */
			ResourceManager.instance().addVM(vm);

		} catch (Exception e) {
			try {
				if (vm != null) {
					msg = String.format("VM#%d preparation failed: %s", vm.getId(), e.getMessage());
					logger.error(msg);
					/* remove this VM instance */
					CloudBroker broker = CloudBroker.createBroker("ONE");
					broker.finalizeVM(vm);
					msg = String.format("VM#%d has been finalized.", vm.getId());
					logger.info(msg);
				}
				else {
					msg = String.format("VM#UNKNOWN preparation failed: %s", e.getMessage());
					logger.error(msg);
				}
			} catch (BrokerException e1) {
				msg = String.format("Failed to finalize VM#%d: %s", vm.getId(), e1.getMessage());
				logger.error(msg);
			}
		}
	}

}
