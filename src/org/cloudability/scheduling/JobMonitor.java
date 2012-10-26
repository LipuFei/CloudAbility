/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import org.apache.log4j.Logger;

import org.cloudability.DataManager;
import org.cloudability.analysis.JobProfiler;
import org.cloudability.analysis.StatisticsData;
import org.cloudability.analysis.StatisticsManager;
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
			String msg = String.format(
					"Interrupted while waiting for JOB#%d: %s.",
					job.getId(), e.getMessage());
			logger.error(msg);
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
			/* store the statistics of the job's performance */
			JobProfiler profiler = job.getProfiler();
			long arrivalTime = profiler.getMark("arrivalTime");
			long makespan = profiler.getMark("finishTime") - profiler.getMark("arrivalTime");
			long waitTime = profiler.getMark("startTime") - profiler.getMark("arrivalTime");
			long runningTime = profiler.getMark("finishTime") - profiler.getMark("startTime");
			long preparationTime = profiler.getPeriod("preparationTime");
			long uploadTime = profiler.getPeriod("uploadTime");
			long tarballExtractionTime = profiler.getPeriod("tarballExtractionTime");
			long executionTime = profiler.getPeriod("executionTime");
			long downloadTime = profiler.getPeriod("downloadTime");

			msg = String.format(
					"JOB#%d: makespan=%d; waitTime=%d; runningTime=%d; preparationTime=%d; uploadTime=%d; tarballExtractionTime=%d; executionTime=%d; downloadTime=%d.",
					job.getId(), makespan, waitTime, runningTime, preparationTime, uploadTime, tarballExtractionTime, executionTime, downloadTime);
			logger.info(msg);

			StatisticsData data = new StatisticsData();
			data.add("arrivalTime", arrivalTime);
			data.add("makespan", makespan);
			data.add("waitTime", waitTime);
			data.add("runningTime", runningTime);
			data.add("preparationTime", preparationTime);
			data.add("uploadTime", uploadTime);
			data.add("tarballExtractionTime", tarballExtractionTime);
			data.add("executionTime", executionTime);
			data.add("downloadTime", downloadTime);

			StatisticsManager.instance().addJobStatistics(job.getId(), data);

		}
		else if (job.getStatus() == JobStatus.FAILED) {
			/* log the failure */
			msg = String.format("JOB#%d has failed.", job.getId());
			logger.info(msg);

			/* put it back into the pending queue again */
			job.setStatus(JobStatus.PENDING);
			DataManager.instance().getPendingJobQueue().addJob(job);
		}
		else if (job.getStatus() == JobStatus.STOPPED) {
			/* log this situation */
			msg = String.format("JOB#%d has been stopped.", job.getId());
			logger.info(msg);

			/* put it back into the pending queue again */
			job.setStatus(JobStatus.PENDING);
			DataManager.instance().getPendingJobQueue().addJob(job);
		}
		else {
			/* unexpected status */
			msg = String.format("unexpected status for JOB#%d.", job.getId());
			logger.info(msg);
		}
	}

}
