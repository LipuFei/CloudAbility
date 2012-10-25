/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import java.lang.Thread.State;
import java.util.Iterator;
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

		String msg = "Scheduler has started.";
		logger.debug(msg);
		try {
			while (true) {
				/* show the system status */
				showStatus();

				/* check toStop signal */
				if (this.toStop) break;

				/* Scheduler's regular check */
				regularCheck();
				/* Resource manager's regular check */
				ResourceManager.instance().regularCheck();

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
				msg = String.format("VM#%d has been selected.", vm.getId());
				logger.debug(msg);

				/* select a job and assign a VM instance to it */
				Job job = this.allocator.select();
				if (job == null) {
					/*
					 * The selected VM has invoked assign(), so if there is
					 * no job available, we need to free it.
					 */
					synchronized (vm) {
						msg = String.format("No jobs available, freeing VM#%d.", vm.getId());
						logger.debug(msg);
						vm.free();
					}
					synchronized (pendingQueue) {
						pendingQueue.wait(defaultWaitInterval);
					}
					continue;
				}
				msg = String.format("JOB#%d has been selected.", job.getId());
				logger.debug(msg);

				/* assign VM instance and execute the job */
				job.setVMInstance(vm);
				createJobMonitor(job);
			}

			/* finalize */
			logger.info("Scheduler is finalizing.");
			finialize();

			logger.info("Scheduler is done.");
		} catch (InterruptedException e) {
			msg = String.format("Interrupted while waiting: %s.", e.getMessage());
			logger.error(msg);
		}
	}


	/**
	 * Finalization. This method is called when scheduler is required to be
	 * stooped. It waits for all JobMonitors to stop the Jobs they are
	 * monitoring. 
	 */
	private void finialize() {
		LinkedList<JobMonitor> monitorList = DataManager.instance().getJobMonitorList();
		synchronized (monitorList) {
			Iterator<JobMonitor> itr = monitorList.iterator();
			while (itr.hasNext()) {
				JobMonitor monitor = itr.next();
				monitor.setToStop();
				try {
					monitor.join();
				} catch (InterruptedException e) {
					String msg = String.format("Interrupted while waiting for JobMonitor to stop: %s.", e.getMessage());
					logger.error(msg);
				}
			}
		}
	}


	/**
	 * Regular check. It removes all finished JobMonitors.
	 */
	private void regularCheck() {
		/* remove all finished JobMonitors */
		LinkedList<JobMonitor> monitorList = DataManager.instance().getJobMonitorList();
		synchronized (monitorList) {
			Iterator<JobMonitor> itr = monitorList.iterator();
			while (itr.hasNext()) {
				JobMonitor monitor = itr.next();
				if (monitor.getState() == State.TERMINATED) {
					itr.remove();
					String msg = "Finished JobMonitor removed.";
					logger.debug(msg);
				}
			}
		}
	}


	/**
	 * A Method to create a JobMonitor for a Job in order to execute and
	 * monitor it.
	 * @param job The job to be executed and monitored.
	 */
	private void createJobMonitor(Job job) {
		String msg = String.format(
				"Starting job monitor for JOB#%d on VM#%d.",
				job.getId(), job.getVMInstance().getId());
		logger.debug(msg);

		/* create a job monitor thread */
		JobMonitor jobMonitor = new JobMonitor(job);
		jobMonitor.start();

		/* add into list */
		DataManager.instance().addJobMonitor(jobMonitor);
	}


	/**
	 * Shows the status of the system, including how many JobMonitors are
	 * running, how many VMInstances do we have, etc.
	 */
	private void showStatus() {
		String msg = String.format(
				"Jobs in pending queue: %d.",
				DataManager.instance().getPendingJobQueue().size());
		logger.info(msg);
		msg = String.format(
				"JobMonitors running: %d.",
				DataManager.instance().getJobMonitorNumber());
		logger.info(msg);
		msg = String.format(
				"VMInstances in resource pool: %d.",
				ResourceManager.instance().getVMInstanceNumber());
		logger.info(msg);
		msg = String.format(
				"VMAgents running: %d.",
				ResourceManager.instance().getVMAgentNumber());
		logger.info(msg);
	}

}
