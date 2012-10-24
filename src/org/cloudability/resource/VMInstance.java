/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.cloudability.broker.CloudBroker;
import org.cloudability.util.BrokerException;

/**
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class VMInstance {

	private Logger logger;

	/* Statuses of a VM instance */
	public enum VMStatus {
		PENDING, BOOTING, RUNNING, SHUTDOWN, UNKNOWN
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
		this.logger = Logger.getLogger(VMInstance.class);
		setId(id);
		setStatus(VMStatus.UNKNOWN);
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
	 * A blocking method that waits for this VM instance to be ready to use.
	 * @param timeout The time out for waiting.
	 * @return true if the VM instance is ready, false otherwise.
	 * @throws BrokerException 
	 * @throws InterruptedException 
	 */
	public void waitForReady() throws BrokerException, InterruptedException {
		if (this.status == VMStatus.SHUTDOWN || this.status == VMStatus.UNKNOWN) {
			return;
		}

		/* first wait until running */
		CloudBroker broker = CloudBroker.createBroker("ONE");
		while (this.status != VMStatus.RUNNING) {
			broker.updateInfo(this);
			Thread.sleep(1000);
		}
	}

	/**
	 * 
	 * @param timeout
	 * @return
	 * @throws VMException
	 */
	public boolean ping(int timeout) throws VMException {
		/* ping the VM */
		String msg = String.format(
				"Pinging VM#%d %s...", id, ipAddress);
		logger.debug(msg);
		try {
			InetAddress addr = InetAddress.getByName(ipAddress);
			return addr.isReachable(timeout);
		} catch (UnknownHostException e) {
			msg = String.format(
					"VM#%d Unknown host exception while waiting: %s.",
					id, e.getMessage());
			logger.error(msg);
			throw new VMException(msg);
		} catch (IOException e) {
			msg = String.format(
					"VM#%d IO exception while waiting: %s.",
					id, e.getMessage());
			logger.error(msg);
			throw new VMException(msg);
		}
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
