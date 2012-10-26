/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource.policy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.cloudability.DataManager;
import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMInstance;
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

	/**
	 * Check if total number of VMs
	 */
	@Override
	public void regularCheck() {
		int currentNumber = ResourceManager.instance().getVMAgentNumber() +
				ResourceManager.instance().getVMInstanceNumber();
		int toAllocate = allocationNumber - currentNumber;
		while (toAllocate-- > 0) {
			ResourceManager.instance().allocateVM();
		}
	}

}
