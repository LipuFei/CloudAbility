/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import java.util.concurrent.atomic.AtomicInteger;

import org.cloudability.analysis.Profiler;

/**
 * The VM instance object.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class VMInstance {

	/* Statuses of a VM instance */
	public enum VMStatus {
		PENDING, BOOTING, RUNNING, SHUTDOWN, UNKNOWN
	}

	private Profiler profiler = new Profiler();

	private int id;
	private VMStatus status;
	private String ipAddress;

	/* number of jobs assigned to this VM */
	private AtomicInteger jobsAssigned;

	/* the last time this VM instance becomes idle */
	private long lastTimeBecomesIdle;
	private long aggregateIdleTime;

	/**
	 * Constructor.
	 * @param id ID of this VM instance.
	 */
	public VMInstance(int id) {
		setId(id);
		setStatus(VMStatus.UNKNOWN);
		this.jobsAssigned = new AtomicInteger(0);

		this.lastTimeBecomesIdle = System.currentTimeMillis();
		this.aggregateIdleTime = 0;
	}

	public Profiler getProfiler() {
		return this.profiler;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;

		VMInstance vm = (VMInstance)obj;
		if (this.id == vm.id) return true;

		return false;
	}

	public long updateAggregateIdleTime() {
		this.aggregateIdleTime =
				System.currentTimeMillis() - this.lastTimeBecomesIdle;
		return this.aggregateIdleTime;
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
	 * Occupies this VM instance. It increase the number of jobs assigned to
	 * this VM instance.
	 */
	public void occupy() {
		this.profiler.mark("idleTime");
		this.jobsAssigned.incrementAndGet();
		this.profiler.mark("busyTime");
	}

	/**
	 * Frees a job to this VM instance.
	 */
	public void free() {
		this.profiler.mark("busyTime");
		this.profiler.mark("idleTime");
		this.jobsAssigned.decrementAndGet();
		this.lastTimeBecomesIdle = System.currentTimeMillis();

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
