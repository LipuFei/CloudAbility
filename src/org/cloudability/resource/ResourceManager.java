/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import org.cloudability.CentralManager;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.VMState;
import org.cloudability.resource.policy.Provisioner;
import org.cloudability.resource.policy.SimpleElasticProvisioner;
import org.cloudability.resource.policy.StaticProvisioner;
import org.cloudability.util.CloudConfigException;
import org.cloudability.util.CloudLogger;

/**
 * A singleton that maintains the VM resources and performs provisioning
 * policy.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class ResourceManager {

	private final static Logger logger = CloudLogger.getSystemLogger();

	/* the only instance */
	private static ResourceManager _instance;

	/* list of VM instances */
	private LinkedList<VMInstance> vmList;

	/* some nasty stuff */
	private ExecutorService vmAgentExecutorService;
	private volatile int vmAgentNumber;

	/* provisioning policy */
	private Provisioner provisioner;


	/**
	 * Gets the instance.
	 * @return the ResourceManager instance.
	 */
	public static ResourceManager instance() {
		return _instance;
	}

	/**
	 * Constructor.
	 * @throws CloudConfigException
	 */
	public ResourceManager() throws CloudConfigException {
		this.vmList = new LinkedList<VMInstance>();

		this.vmAgentExecutorService = Executors.newScheduledThreadPool(5);
		this.vmAgentNumber = 0;
	}

	/**
	 * Initialization.
	 * @throws CloudConfigException
	 */
	public static void initialize() throws CloudConfigException {
		_instance = new ResourceManager();

		/* initialize provisioning policy */
		HashMap<String, String> configMap =
				CentralManager.instance().getConfigMap();
		String provisionerName = configMap.get("PROVISION.POLICY");

		if (provisionerName.equals("STATIC")) {
			_instance.provisioner = new StaticProvisioner();
		}
		else if (provisionerName.equals("SIMPLE_ELASTIC")) {
			_instance.provisioner = new SimpleElasticProvisioner();
		}
		/* Unknown provisioning policy */
		else {
			String msg = String.format("Unknown provisioning policy name: %s.", provisionerName);
			logger.error(msg);
			throw new CloudConfigException(msg);
		}

		String info = "Resource Manager has been initialized.";
		logger.info(info);
	}

	/**
	 * Finalizes the resource manager.
	 */
	public static void cleanup() {
		try {
			/* stop all VM Agents */
			String msg = "Stopping all VM Agents...";
			logger.info(msg);
			_instance.vmAgentExecutorService.shutdownNow();
			_instance.vmAgentExecutorService.awaitTermination(5, TimeUnit.SECONDS);

			/* release all VMs */
			msg = "Releasing all VM instances...";
			logger.info(msg);
			Iterator<VMInstance> itrVM = _instance.vmList.iterator();
			while (itrVM.hasNext()) {
				VMInstance vm = itrVM.next();
				vm.terminate();
				StatisticsManager.instance().addVMProfiler(vm.getId(), vm.getProfiler());
			}

			_instance.vmList.clear();
		} catch (InterruptedException e) {
			String msg = String.format(
					"Provisioner thread interrupted while joining: %s.",
					e.getMessage());
			logger.error(msg);
		} catch (Exception e) {
			String msg = String.format("Unable to release VM instances: %s.",
					e.getMessage());
			logger.error(msg);
		}
	}


	/**
	 * Regular check. It removes all finished VM Agents and updates the status
	 * of VM instances.
	 */
	public void regularCheck() {
		/* update VM status and remove unusable VMs */
		synchronized (vmList) {
			try {
				Iterator<VMInstance> itr = vmList.iterator();
				while (itr.hasNext()) {
					VMInstance vm = itr.next();

					synchronized (vm) {
						if (vm.isIdle() && vm.getStatus() != VMState.RUNNING) {
							vm.terminate();
							itr.remove();

							logger.debug("VM removed.");

							StatisticsManager.instance().addVMProfiler(vm.getId(), vm.getProfiler());
							StatisticsManager.instance().addFinalizedVM();
						}
					}
				}
			} catch (Exception e) {
				String msg = String.format("Error during regular check: %s", e.getMessage());
				logger.error(msg);
			}
		}
	}


	public void provisionerRegularCheck() {
		this.provisioner.regularCheck();
	}


	/**
	 * The public interface for allocating a VM instance. It starts a VM Agent
	 * which will allocate and prepare a VM instance until it is ready to go.
	 */
	public synchronized void allocateVM() {
		this.vmAgentNumber++;
		this.vmAgentExecutorService.execute(new VMAgent());

		/* profiling */
		StatisticsManager.instance().addVMAllocationAttempts();
	}

	/**
	 * This method is intended for the VMAgents to update the information when
	 * they finishes or fails.
	 */
	public void reduceVMAgentNumber() {
		this.vmAgentNumber--;
	}

	/**
	 * Gets the number of VMAgents running.
	 * @return The number of VMAgents running.
	 */
	public int getVMAgentNumber() {
		return this.vmAgentNumber;
	}


	/**
	 * Public interface for adding a VM instance into the resource list. It
	 * method to let VMAgents add the prepared VMs into the resource list.
	 * @param vm The VM instance to be added.
	 */
	public void addVM(VMInstance vm) {
		synchronized (vmList) {
			vmList.add(vm);
			String msg = String.format(
					"VM#%d has been added to the resource pool.",
					vm.getId());
			logger.info(msg);
			vmList.notifyAll();
		}

		StatisticsManager.instance().addAllocatedVM();
	}

	/**
	 * Public interface for removing a VM instance from the resource list.
	 * @param vm The VM instance to be removed.
	 */
	public void removeVM(VMInstance vm) {
		synchronized (vmList) {
			vmList.remove(vm);
		}
	}

	/**
	 * Gets the list of VM instances.
	 * @return The list of VM instances.
	 */
	public LinkedList<VMInstance> getVMList() {
		return this.vmList;
	}

	/**
	 * Gets the number of VM instances in the resource list.
	 * @return The number of VM instances in the resource list.
	 */
	public int getResourceNumber() {
		synchronized (this.vmList) {
			return this.vmList.size();
		}
	}


	/**
	 * Asks the resource manager to retrieve an available VM instance according
	 * to the provisioning policy.
	 * @return An available VM instance, null if no available VM instance.
	 */
	public VMInstance getAvailableVM() {
		VMInstance vm = this.provisioner.select();
		return vm;
	}

}
