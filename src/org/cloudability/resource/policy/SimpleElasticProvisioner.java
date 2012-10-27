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
 * A simple elastic provisioner. This provisioner allocates more VMs when the
 * number of jobs in the pending queue reaches a certain level. It also release
 * VMs that are not currently in use when pending queue gets empty.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class SimpleElasticProvisioner extends Provisioner {

	private int pendingJobThreshold;
	private int minVMs;
	private int maxVMs;

	/**
	 * @throws CloudConfigException
	 */
	public SimpleElasticProvisioner() throws CloudConfigException {
		super();
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#parseParameters()
	 */
	@Override
	protected void parseParameters() throws CloudConfigException {
		HashMap<String, String> configMap =
				DataManager.instance().getConfigMap();
		try {
			this.pendingJobThreshold =
					Integer.parseInt(configMap.get("PROVISION.SIMPLE_ELASTIC.PENDINGJOB_THRESHOLD"));
			this.minVMs =
					Integer.parseInt(configMap.get("PROVISION.SIMPLE_ELASTIC.MIN_VMS"));
			this.maxVMs =
					Integer.parseInt(configMap.get("PROVISION.SIMPLE_ELASTIC.MAX_VMS"));
		} catch (NumberFormatException e) {
			String error = String.format(
					"Invalid value \"%s\" for %s",
					configMap.get("PROVISION.STATIC.TOTALNUM"),
					"PROVISION.STATIC.TOTALNUM");
			throw new CloudConfigException(error);
		}
	}

	/**
	 * Allocates the minimum number of VM instances.
	 */
	@Override
	protected void initialize() throws RuntimeException {
		for (int i = 0; i < minVMs; i++) {
			ResourceManager.instance().allocateVM();
		}
	}

	/**
	 * Does elastic provisioning.
	 */
	@Override
	public void regularCheck() {
		int pendingJobNumber = DataManager.instance().getPendingJobQueue().size();
		int runningJobNumber = DataManager.instance().getRunningJobNumber();
		int currentVMNumber = ResourceManager.instance().getVMAgentNumber() +
				ResourceManager.instance().getResourceNumber();

		/*
		 * first make sure that total number of VMs should not below minimum
		 */
		if (currentVMNumber < minVMs) {
			for (int i = currentVMNumber; i < minVMs; i++) {
				ResourceManager.instance().allocateVM();
			}
		}

		/*
		 * if too many pending jobs, allocate one more VM at a time until it
		 * reaches the maximum
		 */
		if (pendingJobNumber > pendingJobThreshold &&
				pendingJobNumber + runningJobNumber < currentVMNumber &&
				currentVMNumber < maxVMs) {
			ResourceManager.instance().allocateVM();
		}
		/*
		 * if no pending jobs, delete one VM at a time until it reaches the
		 * minimum
		 */
		else if (pendingJobNumber == 0 && currentVMNumber > minVMs) {
			/* remove the VM with the most aggregate idle time */
			LinkedList<VMInstance> vmList = ResourceManager.instance().getVMList();
			VMInstance vmToRemove = null;
			synchronized (vmList) {
				Iterator<VMInstance> itr = vmList.iterator();

				/* find a first idle VM. */
				while (itr.hasNext()) {
					VMInstance vm = itr.next();
					if (vm.getJobsAssigned() == 0) {
						vmToRemove = vm;
						break;
					}
				}

				/* find the VM with the highest aggregate idle time */
				while (itr.hasNext()) {
					VMInstance vm = itr.next();
					if (vm.getJobsAssigned() == 0 &&
							vmToRemove.updateAggregateIdleTime() < vm.updateAggregateIdleTime()) {
						vmToRemove = vm;
					}
				}
			}

			if (vmToRemove != null)
				try {
					vmToRemove.terminate();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#preprocess()
	 */
	@Override
	protected void preprocess() {
		// TODO Auto-generated method stub

	}

	/**
	 * Picks the first available VM instance.
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

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#postprocess()
	 */
	@Override
	protected void postprocess() {
		// TODO Auto-generated method stub

	}

}
