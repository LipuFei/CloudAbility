/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling.policy;

import org.cloudability.CentralManager;
import org.cloudability.scheduling.Job;
import org.cloudability.scheduling.JobQueue;

/**
 * The base class of the allocation policies. By invoking select(), a job and
 * a VM instance assigned to it can be retrieved.
 * @author Lipu Fei
 * @version 1.0
 *
 */
public abstract class Allocator {

	protected JobQueue pendingQueue;
	/* the selected job */
	protected Job selectedJob;

	/**
	 * Constructor.
	 */
	public Allocator() {
		this.pendingQueue = CentralManager.instance().getPendingJobQueue();
		this.selectedJob = null;
	}

	/**
	 * The public interface that returns a selected job and a selected VM.
	 * @return A job and a VM instance assigned to it if succeeded; null
	 *         otherwise.
	 */
	public Job select() {
		this.selectedJob = null;
		preprocess();
		allocate();
		postprocess();
		return this.selectedJob;
	}

	/**
	 * This method is invoked before allocation.
	 */
	protected abstract void preprocess();
	/**
	 * The allocation method.
	 */
	protected abstract void allocate();
	/**
	 * This method is invoked after allocation.
	 */
	protected abstract void postprocess();

}
