/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import org.cloudability.DataManager;
import org.cloudability.adapter.Adapter;
import org.cloudability.adapter.AdapterException;
import org.cloudability.analysis.Profiler;
import org.cloudability.analysis.Recorder;

/**
 * The VM instance object.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class VMInstance {

	/* the cloud adapter for this VM instance */
	protected Adapter adapter;

	/* for profiling */
	protected Recorder recorder = new Recorder();
	protected Profiler profiler = new Profiler();

	/* major information */
	protected int id;
	protected VMState state;
	protected String ipAddress;

	/* number of jobs assigned to this VM */
	protected volatile int jobsAssigned;

	/* the last time this VM instance becomes idle */
	protected long lastTimeBecomesIdle;
	protected long aggregateIdleTime;


	/**
	 * Constructor.
	 * @throws Exception 
	 */
	public VMInstance(Adapter adapter) throws Exception {
		/* get adapter, allocate a VM instance and update info. */
		this.adapter = adapter;
		this.id = this.adapter.allocateVM();
		this.updateInfo();

		/* other initializations */
		this.jobsAssigned = 0;
		this.lastTimeBecomesIdle = System.currentTimeMillis();
		this.aggregateIdleTime = 0;

		/* profiling */
		this.getProfiler().mark("startTime");
	}

	public Recorder getRecorder() {
		return this.recorder;
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

	public int getId() {
		return this.id;
	}

	public void setStatus(VMState status) {
		this.state = status;
	}
	public VMState getStatus() {
		return this.state;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}


	/**
	 * This OpenNebula VM instance uses XML-RPC API method "one.vm.info" to
	 * retrieve and update its information. Currently, only two pieces of
	 * information are updated in this method: one is the VM state, and the
	 * other is its public IP address.
	 * @throws Exception The adapter fails or the response is negative. It can
	 *                   also be the failure of parsing the XML document.
	 */
	public void updateInfo() throws Exception {
		String content = this.adapter.infoVM(this.id);
		/* parse the content */
		try {
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc =
					builder.parse(new ByteArrayInputStream(content.getBytes()));
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();

			/* parse IP address */
			XPathExpression expr =
					xpath.compile("/VM/TEMPLATE/CONTEXT/IP_PUBLIC/text()");
			this.ipAddress = expr.evaluate(doc);

			/* parse state */
			expr = xpath.compile("/VM/STATE/text()");
			int state = Integer.parseInt(expr.evaluate(doc));

			/* check state */
			switch (state) {
			case 1: this.state = VMState.PENDING; break;
			case 2: this.state = VMState.BOOTING; break;
			case 3: this.state = VMState.RUNNING; break;
			default:
				this.state = VMState.INVALID; break;
			}

		} catch (Exception e) {
			String msg = String.format(
					"Failed to parse VM info XML. Message=%s.",
					e.getMessage()
			);
			throw new RuntimeException(msg);
		}
	}


	/**
	 * This OpenNebula VM instance uses XML-RPC API method "one.vm.action" with
	 * parameter "finalize" to terminate this VM instance.
	 * @throws Exception The adapter fails or the response is negative.
	 */
	public void terminate() throws Exception {
		this.adapter.finalizeVM(this.id);

		/* profiling */
		this.getProfiler().mark("idleTime");
		this.getProfiler().mark("deadTime");
	}


	/**
	 * Assigns a job to this VM instance. It increase the number of jobs
	 * assigned to this VM instance.
	 */
	public void assign() {
		/* increases the count */
		this.jobsAssigned++;

		/* profiling */
		this.profiler.mark("idleTime");
		this.profiler.mark("busyTime");
		this.recorder.count("jobsAssigned");
	}

	/**
	 * Frees a job to this VM instance.
	 */
	public void free() {
		/* decreases the count */
		this.jobsAssigned--;

		/* update time stamp of becoming idle */
		this.lastTimeBecomesIdle = System.currentTimeMillis();

		/* profiling */
		this.profiler.mark("busyTime");
		this.profiler.mark("idleTime");

		/* notify all */
		synchronized (ResourceManager.instance().getVMList()) {
			ResourceManager.instance().getVMList().notifyAll();
		}
	}

	/**
	 * Get the number of jobs running on this VM instance.
	 */
	public int getJobsAssigned() {
		return this.jobsAssigned;
	}

}
