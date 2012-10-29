/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import org.cloudability.CentralManager;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMInstance;
import org.cloudability.scheduling.policy.Allocator;
import org.cloudability.scheduling.policy.FCFSAllocator;
import org.cloudability.util.CloudLogger;

/**
 * The scheduler.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class Scheduler implements Runnable {

	private final static Logger logger = CloudLogger.getSystemLogger();

	private final static int defaultWaitInterval = 1000;

	private Allocator allocator;

	/**
	 * Constructor.
	 */
	public Scheduler() {
		/* initialize an allocator */
		this.allocator = new FCFSAllocator();
	}

	/**
	 * The scheduling loop.
	 */
	@Override
	public void run() {
		JobQueue pendingQueue = CentralManager.instance().getPendingJobQueue();

		String msg = "Scheduler has started.";
		logger.debug(msg);
		try {
			while (true) {
				/* Scheduler's regular check */
				this.regularCheck();

				/* Resource manager's regular check */
				ResourceManager.instance().regularCheck();

				/* update job status, such as wait time, priority, etc. */
				CentralManager.instance().updateSystemStatus();

				/* provisioner's regular check */
				ResourceManager.instance().provisionerRegularCheck();

				/* get an available VM */
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

				/* select a job */
				Job job = this.allocator.select();
				if (job == null) {
					synchronized (pendingQueue) {
						pendingQueue.wait(defaultWaitInterval);
					}
					continue;
				}

				/* occupy the VM and execute the job on it */
				vm.assign();
				job.setVMInstance(vm);
				CentralManager.instance().executeJob(job);
			}
		} catch (InterruptedException e) {
			msg = String.format("Interrupted while waiting: %s.", e.getMessage());
			logger.warn(msg);
		} finally {
			/* finalize */
			logger.info("Scheduler is finalizing.");
			finialize();
			logger.info("Scheduler is done.");
		}
	}


	/**
	 * Finalization. This method is called when scheduler is required to be
	 * stooped. It waits for all JobMonitors to stop the Jobs they are
	 * monitoring. 
	 */
	private void finialize() {
		CentralManager.cleanup();
	}


	/**
	 * Check finished jobs. Record succeed jobs and deal with failed jobs.
	 */
	private void regularCheck() {
		LinkedList<Job> jobList = CentralManager.instance().getFinishedJobQueue();
		synchronized (jobList) {
			Iterator<Job> itr = jobList.iterator();
			while (itr.hasNext()) {
				Job job = itr.next();

				/* record succeeded jobs */
				if (job.getState() == JobState.FINISHED) {
					StatisticsManager.instance().recordJob(job);
				}
				/* handle failed jobs */
				else if (job.getState() == JobState.FAILED) {
					CentralManager.instance().getPendingJobQueue().addJob(job);
					StatisticsManager.instance().addJobsFailure();
				}
				/* unexpected status */
				else {
					String msg = "Unexpected job status in finished job queue.";
					logger.warn(msg);
				}

				itr.remove();
			}
		}
	}

}
