/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import org.cloudability.DataManager;
import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMInstance;
import org.cloudability.scheduling.policy.Allocator;
import org.cloudability.scheduling.policy.FCFSAllocator;

/**
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class Scheduler implements Runnable {

	private final static int defaultWaitInterval = 1000;

	private Logger logger;

	/* a signal indicates if to stop the scheduler */
	private boolean toStop;

	private Allocator allocator;

	/**
	 * Constructor.
	 */
	public Scheduler() {
		this.logger = Logger.getLogger(Scheduler.class);

		this.toStop = false;

		/* initialize an allocator */
		this.allocator = new FCFSAllocator();
	}

	public void setToStop() {
		this.toStop = true;
	}

	/**
	 * The scheduling loop.
	 */
	@Override
	public void run() {
		JobQueue pendingQueue = DataManager.instance().getPendingJobQueue();

		String msg = "";
		logger.debug("Scheduler has started.");
		try {
			while (true) {
				/* check toStop signal */
				if (this.toStop) break;

				/* check if pending job queue is empty */
				msg = "Waiting...";
				logger.debug(msg);

				/* TODO: update job status, such as wait time, priority, etc. */


				/* check available VM */
				VMInstance vm = ResourceManager.instance().getAvailableVM();
				if (vm == null) {
					/* wait for available resource notification */
					LinkedList<VMInstance> vmList =
						ResourceManager.instance().getVMList();
					synchronized (vmList) {
						vmList.wait(defaultWaitInterval);
					}
					continue;
				}

				/* check pick pending job */
				if (pendingQueue.isEmpty()) {
					synchronized (pendingQueue) {
						pendingQueue.wait(defaultWaitInterval);
					}
					continue;
				}

				/* select a job and assign a VM instance to it */
				Job job = this.allocator.select();
				vm.assign();	/* a MUST */
				job.setVMInstance(vm);

				/* execute */
				msg = String.format(
						"Starting job monitor for JOB#%d on VM#%d.",
						job.getId(), job.getVMInstance().getId());
				logger.debug(msg);
				JobMonitor jobMonitor = new JobMonitor(job);
				Thread monitorThread = new Thread(jobMonitor);
				monitorThread.start();
			}

			logger.debug("Scheduler is stopping.");

			/* TODO: finalize */

			logger.debug("Scheduler is done.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
