/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class VMInstance {

	/* Statuses of a VM instance */
	public enum VMStatus {
		PENDING, RUNNING, SHUTDOWN
	}

	private int id;
	private VMStatus status;
	private String ipAddress;

	private AtomicInteger jobsAssigned;	/* number of jobs assigned to this VM */

	/**
	 * Constructor.
	 * @param id ID of this VM instance.
	 */
	public VMInstance(int id) {
		setId(id);
		setStatus(VMStatus.PENDING);
		this.jobsAssigned = new AtomicInteger(0);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;

		VMInstance vm = (VMInstance)obj;
		if (this.id == vm.id) return true;

		return false;
	}

	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return this.id;
	}

	public void setStatus(VMStatus status) {
		this.status = status;
	}
	public VMStatus getStatus() {
		return this.status;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getIpAddress() {
		return this.ipAddress;
	}

	/**
	 * Assigns a job to this VM instance.
	 */
	public void assign() {
		this.jobsAssigned.incrementAndGet();
	}

	/**
	 * Frees a job to this VM instance.
	 */
	public void free() {
		this.jobsAssigned.decrementAndGet();
		/* notify all */
		synchronized (ResourceManager.instance().getVMList()) {
			ResourceManager.instance().getVMList().notifyAll();
		}
	}

	/**
	 * Get the number of jobs running on this VM instance.
	 */
	public int getJobsAssigned() {
		return this.jobsAssigned.get();
	}

}
