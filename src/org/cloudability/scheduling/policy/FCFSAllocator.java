/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling.policy;

import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMInstance;
import org.cloudability.scheduling.Job;

/**
 * First-Come-First-Server allocation policy. This policy sorts the pending job
 * queue by arrival times, and selects the earliest arrived job.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class FCFSAllocator extends Allocator {

	/**
	 * Constructor. Does nothing, just invokes the super class's constructor.
	 */
	public FCFSAllocator() {
		super();
	}

	/**
	 * Does nothing.
	 */
	@Override
	protected void preprocess() {
	}

	/**
	 * Selects the earliest arrived job and retrieve a VM instance using
	 * provisioning policy
	 */
	@Override
	protected void allocate() {
		/* sort the queue */
		pendingQueue.sort(new FCFSJobComparator());

		/* remove and get the first job, and assign VM instance to it */
		Job job = pendingQueue.popJob();

		this.selectedJob = job;
	}

	/**
	 * Does nothing.
	 */
	@Override
	protected void postprocess() {
	}

}
