/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import org.apache.log4j.Logger;

import org.cloudability.resource.VMInstance;

/**
 * A data class for job that maintains all data related to it. An executable
 * thread can also be created from itself.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class Job implements Runnable {

	/* auto generating job ID */
	private static int maxJobId = 0;

	/* a signal that indicates if this job should stop */
	private volatile boolean toStop;

	/* Statuses of a job */
	public enum JobStatus {
		PENDING, RUNNING, FINISHED, FAILED, STOPPED
	}

	private int id;
	private JobStatus status;
	private VMInstance vmInstance;

	/* execution related */
	private String application;
	private String parameters;

	/* other information needed by other modules */
	private long arrivalTime;
	private long executionTime;
	private long waitTime;

	private int priority;
	private int failure;

	/**
	 * Constructor.
	 * @param id ID of this job.
	 */
	public Job(int id) {
		this.toStop = false;
		this.status = JobStatus.PENDING;

		this.id = id;
		this.vmInstance = null;
		this.application = "";
		this.parameters = "";
	}

	/**
	 * Constructor.
	 * @param id ID of this job.
	 * @param application The application to run.
	 * @param parameters Parameters for the application.
	 */
	public Job(int id, String application, String parameters) {
		this.toStop = false;
		this.status = JobStatus.PENDING;

		this.id = id;
		this.vmInstance = null;
		this.application = application;
		this.parameters = parameters;
	}

	/**
	 * Generates a job ID.
	 * @return A job ID.
	 */
	public synchronized static int generateJobID() {
		return maxJobId++;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;

		Job job = (Job)obj;

		if (this.id != job.id) return false;

		return true;
	}

	/**
	 * Sets the toStop signal.
	 */
	public void setToStop() {
		this.toStop = true;
	}

	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return this.id;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}
	public JobStatus getStatus() {
		return this.status;
	}

	public void setVMInstance(VMInstance vmInstance) {
		this.vmInstance = vmInstance;
	}
	public VMInstance getVMInstance() {
		return this.vmInstance;
	}

	public String getApplication() {
		return this.application;
	}
	public String getParameters() {
		return this.parameters;
	}

	public void setArrivalTime(long arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public long getArrivalTime() {
		return this.arrivalTime;
	}


	/**
	 * Executes this job.
	 */
	@Override
	public void run() {
		Logger logger = Logger.getLogger(Job.class);
		String info = "";

		/* change status to running */
		info = String.format("Job#%d started running.", id);
		logger.debug(info);
		this.status = JobStatus.RUNNING;

		/* running */
		info = String.format("Job#%d is running.", id);
		logger.debug(info);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* finish */
		info = String.format("Job#%d is finished.", id);
		logger.debug(info);
		this.status = JobStatus.FINISHED;

		/* notify all */
		synchronized (this) {
			this.notifyAll();
		}
	}

}
