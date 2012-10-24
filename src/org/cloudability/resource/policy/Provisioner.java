/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource.policy;

import org.cloudability.resource.VMInstance;
import org.cloudability.util.CloudConfigException;

/**
 * A base class for VM provisioning policies. Invoke the select() to retrieve a
 * VM instance.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public abstract class Provisioner implements Runnable {

	/* the selected VM instance. */
	protected VMInstance selectedVM;

	/* stop signal */
	protected boolean toStop;

	/**
	 * Constructor.
	 * @throws CloudConfigException
	 */
	public Provisioner() throws CloudConfigException {
		this.selectedVM = null;
		this.toStop = false;
		/* parse parameters */
		parseParameters();
		/* initialize */
		initialize();
	}

	/**
	 * Sets the stop signal.
	 */
	public void setStop() {
		this.toStop = true;
	}

	/**
	 * Parses parameters.
	 * @throws CloudConfigException
	 */
	protected abstract void parseParameters() throws CloudConfigException;

	/**
	 * Initialization.
	 */
	protected abstract void initialize() throws RuntimeException;

	/**
	 * A public interface for selecting a VM instance.
	 * @return A VM instance if any available VM can be found according to the
	 *         provisioning policy; null otherwise.
	 */
	public VMInstance select() {
		this.selectedVM = null;
		preprocess();
		provision();
		postprocess();
		return this.selectedVM;
	}

	/**
	 * This method is invoked before provisioning.
	 */
	protected abstract void preprocess();
	/**
	 * Provisioning.
	 */
	protected abstract void provision();
	/**
	 * This method is invoked after provisioning.
	 */
	protected abstract void postprocess();

}
