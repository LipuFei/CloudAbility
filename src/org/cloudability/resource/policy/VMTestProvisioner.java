/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource.policy;

import java.util.HashMap;
import java.util.LinkedList;

import org.cloudability.CentralManager;
import org.cloudability.resource.ResourceManager;
import org.cloudability.resource.VMInstance;
import org.cloudability.util.CloudConfigException;
import org.cloudability.util.CloudLogger;

/**
 * This module is used to test the allocation and preparation overhead of a VM
 * instance. It allocates only one VM instance and after it is ready, it
 * finalizes it and allocates one again.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class VMTestProvisioner extends Provisioner {

	private int allocationTimes;
	private int testTimes;

	/**
	 * @throws CloudConfigException
	 */
	public VMTestProvisioner() throws CloudConfigException {
		this.allocationTimes = 0;
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#parseParameters()
	 */
	@Override
	protected void parseParameters() throws CloudConfigException {
		HashMap<String, String> configMap =
				CentralManager.instance().getConfigMap();
		try {
			this.testTimes = Integer.parseInt(
					configMap.get("PROVISION.VM_TEST.TEST_TIMES"));
		} catch (NumberFormatException e) {
			String error = String.format(
					"Invalid value \"%s\" for %s",
					configMap.get("PROVISION.STATIC.TOTALNUM"),
					"PROVISION.STATIC.TOTALNUM");
			throw new CloudConfigException(error);
		}
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#initialize()
	 */
	@Override
	protected void initialize() throws RuntimeException {
		/* allocate only one VM instance */
		ResourceManager.instance().allocateVM();
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#regularCheck()
	 */
	@Override
	public void regularCheck() {
		int vmNumber = ResourceManager.instance().getResourceNumber();
		int vmAgentNumber = ResourceManager.instance().getVMAgentNumber();

		/* check if the VM instance is ready */
		if (vmNumber == 1) {
			LinkedList<VMInstance> vmList =
					ResourceManager.instance().getVMList();
			synchronized (vmList) {
				VMInstance vm = ResourceManager.instance().getVMList().pop();
				try {
					vm.terminate();
				} catch (Exception e) {
					
					e.printStackTrace();
				}

				/* exit if enough tests have been done */
				if (++this.allocationTimes < testTimes) {
					ResourceManager.instance().allocateVM();
				}
				else {
					CloudLogger.getSystemLogger().info("VM Test is done.");
				}
			}
		}
		/* check if allocation failed */
		else if (this.allocationTimes < testTimes && vmAgentNumber == 0) {
			ResourceManager.instance().allocateVM();
		}
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#preprocess()
	 */
	@Override
	protected void preprocess() {
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#provision()
	 */
	@Override
	protected void provision() {
	}

	/* (non-Javadoc)
	 * @see org.cloudability.resource.policy.Provisioner#postprocess()
	 */
	@Override
	protected void postprocess() {
	}

}
