/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import org.apache.log4j.Logger;

import com.trilead.ssh2.Session;

import org.cloudability.CentralManager;
import org.cloudability.adapter.Adapter;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.VMState;
import org.cloudability.util.CloudLogger;
import org.koala.internals.SSHException;
import org.koala.internals.SSHHandler;

/**
 * This runnable class is responsible for allocating and monitoring a VM
 * instance to make sure that it is ready before it is added into the resource
 * list.
 * The VMAgent performs four steps to make sure a VM instance is ready:
 *   (1) allocates a new VM instance;
 *   (2) keeps checking its state until it is RUNNING;
 *   (3) keeps pinging the VM instance until it is reachable;
 *   (4) keeps SSHing the VM instance until it is reachable.
 * After all four steps succeeded, this VM instance is ready and is put into the
 * resource pool.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class VMAgent implements Runnable {

	private final static Logger logger = CloudLogger.getSystemLogger();

	/* default timeout is 2 minutes */
	private final static long defaultTimeout = 2 * 60 * 1000;
	/* default wait period is 1 second */
	private final static long defaultWaitPeriod = 1000;


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String msg = "";
		long startTime = System.currentTimeMillis();
		VMInstance vm = null;

		try {
			/* create an adapter and allocates VM instance */
			Adapter adapter = Adapter.createAdapter("ONE");
			vm = new VMInstance(adapter);

			/* wait for the VM instance to be RUNNING */
			vm.getProfiler().mark("bootingTime");
			msg = String.format("Agent is waiting for VM#%d to be RUNNING...", vm.getId());
			logger.debug(msg);
			/* check status first */
			if (vm.getStatus() == VMState.SHUTDOWN || vm.getStatus() == VMState.UNKNOWN) {
				msg = String.format("VM#%d status is invalid.", vm.getId());
				throw new Exception(msg);
			}
			/* wait until it it running */
			while (vm.getStatus() != VMState.RUNNING) {
				vm.updateInfo();

				/* check time out */
				if (System.currentTimeMillis() - startTime > defaultTimeout) {
					msg = String.format("VM#%d preparation timeout.", vm.getId());
					throw new Exception(msg);
				}

				Thread.sleep(defaultWaitPeriod);
			}
			vm.getProfiler().mark("bootingTime");

			/* use ping to check if system has been initialized */
			vm.getProfiler().mark("preparationTime");
			msg = String.format("Pinging VM#%d...", vm.getId());
			logger.debug(msg);
			while (true) {
				String cmd = String.format("ping -c 1 %s", vm.getIpAddress());
				Process process = Runtime.getRuntime().exec(cmd);
				boolean reachable = (process.waitFor() == 0);

				if (reachable) break;

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
					CentralManager.instance().getConfigMap().get("VM.USERNAME");
			boolean sshTest = false;
			while (!sshTest) {
				try {
					Session session = SSHHandler.getSession(vm.getIpAddress(), vmUsername);
					session.close();
					sshTest = true;
				} catch (SSHException e) {
					sshTest = false;
				}

				/* check timeout */
				if (System.currentTimeMillis() - startTime > defaultTimeout) {
					msg = String.format("VM#%d preparation timeout.", vm.getId());
					throw new Exception(msg);
				}

				Thread.sleep(defaultWaitPeriod);
			}
			vm.getProfiler().mark("preparationTime");

			/* VM instance is ready, put it into the resource list */
			ResourceManager.instance().addVM(vm);

			vm.getProfiler().mark("availableTime");
			vm.getProfiler().mark("idleTime");

			long preparationTime = System.currentTimeMillis() - startTime;
			StatisticsManager.instance().addVMPreparationTime(preparationTime);

		} catch (Exception e) {
			try {
				if (vm != null) {
					msg = String.format("VM#%d preparation failed: %s", vm.getId(), e.getMessage());
					logger.error(msg);
					/* remove this VM instance */
					vm.terminate();
					msg = String.format("VM#%d has been finalized.", vm.getId());
					logger.info(msg);
				}
				else {
					msg = String.format("VM#UNKNOWN preparation failed: %s", e.getMessage());
					logger.error(msg);
				}
			} catch (Exception e1) {
				msg = String.format("Failed to finalize VM#%d: %s", vm.getId(), e1.getMessage());
				logger.error(msg);
			}

			StatisticsManager.instance().addVMAllocationFailures();
		} finally {
			ResourceManager.instance().reduceVMAgentNumber();
		}
	}

}
