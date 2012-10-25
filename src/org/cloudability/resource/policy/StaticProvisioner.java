/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource.policy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.cloudability.DataManager;
import org.cloudability.broker.CloudBroker;
import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMAgent;
import org.cloudability.resource.VMInstance;
import org.cloudability.util.BrokerException;
import org.cloudability.util.CloudConfigException;

/**
 * A static provisioning policy that allocates a fixed number of VM instances.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class StaticProvisioner extends Provisioner {

	/* the number of VM instances to be allocated */
	private int allocationNumber;

	/**
	 * Constructor.
	 */
	public StaticProvisioner() throws CloudConfigException {
		super();
	}

	/**
	 * Parses parameters.
	 */
	@Override
	protected void parseParameters() throws CloudConfigException {
		HashMap<String, String> configMap =
				DataManager.instance().getConfigMap();
		try {
			this.allocationNumber = Integer.parseInt(
					configMap.get("PROVISION.STATIC.TOTALNUM"));
		} catch (NumberFormatException e) {
			String error = String.format(
					"Invalid value \"%s\" for %s",
					configMap.get("PROVISION.STATIC.TOTALNUM"),
					"PROVISION.STATIC.TOTALNUM");
			throw new CloudConfigException(error);
		}
	}

	/**
	 * Allocate a fixed number of VM instances.
	 */
	@Override
	protected void initialize() {
		for (int i = 0; i < allocationNumber; i++) {
			ResourceManager.instance().allocateVM();
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void run() {
	}

	/**
	 * Does nothing.
	 */
	@Override
	protected void preprocess() {
	}

	/**
	 * Find the first available VM instance in the resource list.
	 */
	@Override
	protected void provision() {
		LinkedList<VMInstance> vmList = ResourceManager.instance().getVMList();
		synchronized (vmList) {
			Iterator<VMInstance> itr = vmList.iterator();
			while (itr.hasNext()) {
				VMInstance vm = itr.next();
				synchronized (vm) {
					if (vm.getJobsAssigned() == 0) {
						this.selectedVM = vm;
						/* IMPORTANT: assign the VM here is because, the
						 * provisioner thread regularly may release some VM if
						 * they have been idle for a certain amount of time.
						 * So if must use an atomic-like operation to assign
						 * the VM that has been selected before the provisioner
						 * thread can have access to it.
						 */
						vm.assign();
						break;
					}
				}
			}
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	protected void postprocess() {
	}

}
