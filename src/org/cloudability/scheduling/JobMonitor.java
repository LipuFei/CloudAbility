/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import org.apache.log4j.Logger;

import org.cloudability.scheduling.Job.JobStatus;

/**
 * It is responsible for executing a job and monitoring its execution progress.
 * After the job is finished or it is failed, the monitor also needs to do the
 * corresponding post procedures.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class JobMonitor extends Thread {

	private final static int defaultWaitInterval = 1000;

	private Logger logger;

	/* a signal that indicates if this execution should stop */
	private volatile boolean toStop;

	private volatile Job job;

	private Thread jobThread;

	/**
	 * Constructor.
	 * @param job The job to execute.
	 */
	public JobMonitor(Job job) {
		super();

		this.toStop = false;

		this.logger = Logger.getLogger(JobMonitor.class);

		this.job = job;
		this.jobThread = null;
	}

	/**
	 * Sets the toStop signal. The monitor will first stop the job under its
	 * supervision and then stop itself.
	 */
	public void setToStop() {
		this.toStop = true;
	}

	/**
	 * Executes the job and monitors this execution.
	 */
	@Override
	public void run() {
		/* execute the job in another thread */
		jobThread = new Thread(job);
		jobThread.start();

		/* wait on it for success, failure, or stop */
		try {
			while (job.getStatus() != JobStatus.FINISHED &&
					job.getStatus() != JobStatus.FAILED &&
					job.getStatus() != JobStatus.STOPPED) {
				/* check stop signal  */
				if (toStop) {
					job.setToStop();
				}

				synchronized (job) {
					job.wait(defaultWaitInterval);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* free the VM */
		job.getVMInstance().free();
		String msg = String.format(
				"VM#%d has been freed, current util=%d.",
				job.getVMInstance().getId(),
				job.getVMInstance().getJobsAssigned());
		logger.debug(msg);

		/* check job status */
		if (job.getStatus() == JobStatus.FINISHED) {
			/* log the success */

			/* TODO: put it into the finished job queue */
			
		}
		else if (job.getStatus() == JobStatus.FAILED) {
			/* log the failure */

			/* TODO: put it into the pending queue again */
		}
		else if (job.getStatus() == JobStatus.STOPPED) {
			/* log this situation */
		}
		else {
			/* unexpected status */
			
		}
	}

}
