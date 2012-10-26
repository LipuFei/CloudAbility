/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import java.lang.Thread.State;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import org.cloudability.DataManager;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.broker.CloudBroker;
import org.cloudability.resource.VMInstance.VMStatus;
import org.cloudability.resource.policy.Provisioner;
import org.cloudability.resource.policy.SimpleElasticProvisioner;
import org.cloudability.resource.policy.StaticProvisioner;
import org.cloudability.util.BrokerException;
import org.cloudability.util.CloudConfigException;

/**
 * A singleton that maintains the VM resources and performs provisioning
 * policy.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class ResourceManager {

	/* the only instance */
	private static ResourceManager _instance;

	private Logger logger;

	/* list of VM instances */
	private LinkedList<VMInstance> vmList;

	/* some nasty stuff */
	private LinkedList<VMAgent> vmAgentList;

	/* provisioning policy */
	private Provisioner provisioner;


	/**
	 * Constructor.
	 * @throws CloudConfigException
	 */
	public ResourceManager() throws CloudConfigException {
		this.logger = Logger.getLogger(ResourceManager.class);

		this.vmList = new LinkedList<VMInstance>();

		this.vmAgentList = new LinkedList<VMAgent>();
	}


	/**
	 * Initialization.
	 * @throws CloudConfigException
	 */
	public static void initialize() throws CloudConfigException {
		_instance = new ResourceManager();

		/* initialize provisioning policy */
		HashMap<String, String> configMap =
				DataManager.instance().getConfigMap();
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
			_instance.logger.error(msg);
			throw new CloudConfigException(msg);
		}

		String info = "Resource Manager has been initialized.";
		_instance.logger.info(info);
	}


	/**
	 * Gets the instance.
	 * @return the ResourceManager instance.
	 */
	public static ResourceManager instance() {
		return _instance;
	}


	/**
	 * Finalizes the resource manager.
	 */
	public void finalize() {
		try {
			/* stop all VM Agents */
			String msg = "Stopping all VM Agents...";
			_instance.logger.info(msg);
			Iterator<VMAgent> itrAgent = vmAgentList.iterator();
			while (itrAgent.hasNext()) {
				VMAgent agent = itrAgent.next();
				agent.setToStop();
				agent.join();
			}

			/* release all VMs */
			msg = "Releasing all VM instances...";
			_instance.logger.info(msg);
			CloudBroker broker = CloudBroker.createBroker("ONE");
			Iterator<VMInstance> itrVM = _instance.vmList.iterator();
			while (itrVM.hasNext()) {
				VMInstance vm = itrVM.next();
				broker.finalizeVM(vm);

				vm.getProfiler().mark("idleTime");
				vm.getProfiler().mark("deadTime");
				StatisticsManager.instance().addVMProfiler(vm.getId(), vm.getProfiler());
			}

			vmAgentList.clear();
			vmList.clear();
		} catch (InterruptedException e) {
			String msg = String.format(
					"Provisioner thread interrupted while joining: %s.",
					e.getMessage());
			_instance.logger.error(msg);
		} catch (BrokerException e) {
			String msg = String.format("Unable to release VM instances: %s.",
					e.getMessage());
			_instance.logger.error(msg);
		}
	}


	/**
	 * Regular check. It removes all finished VM Agents and updates the status
	 * of VM instances.
	 */
	public void regularCheck() {
		/* remove all terminated VM Agents */
		synchronized (vmAgentList) {
			Iterator<VMAgent> itr = vmAgentList.iterator();
			while (itr.hasNext()) {
				VMAgent agent = itr.next();
				if (agent.getState() == State.TERMINATED) {
					itr.remove();
					logger.debug("Finished VMAgent removed.");
				}
			}
		}

		/* update VM status and remove unusable VMs */
		synchronized (vmList) {
			try {
				CloudBroker broker = CloudBroker.createBroker("ONE");
				Iterator<VMInstance> itr = vmList.iterator();
				while (itr.hasNext()) {
					VMInstance vm = itr.next();

					synchronized (vm) {
						/* skip those who have jobs on them */
						if (vm.getJobsAssigned() > 0) continue;
	
						broker.updateInfo(vm);
						if (vm.getStatus() != VMStatus.RUNNING) {
							broker.finalizeVM(vm);
							itr.remove();

							logger.debug("VM removed.");

							vm.getProfiler().mark("idleTime");
							vm.getProfiler().mark("deadTime");
							StatisticsManager.instance().addVMProfiler(vm.getId(), vm.getProfiler());

							StatisticsManager.instance().addFinalizedVM();
						}
					}
				}
			} catch (BrokerException e) {
				String msg = String.format("Error during regular check: %s", e.getMessage());
				logger.error(msg);
			}
		}
	}


	public void provisionerRegularCheck() {
		this.provisioner.regularCheck();
	}


	/**
	 * Gets the number of VMInstances in the resource pool.
	 * @return The number of VMInstances in the resource pool.
	 */
	public int getVMInstanceNumber() {
		synchronized (this.vmList) {
			return this.vmList.size();
		}
	}


	/**
	 * Gets the number of VMAgents running.
	 * @return The number of VMAgents running.
	 */
	public int getVMAgentNumber() {
		synchronized (this.vmAgentList) {
			return this.vmAgentList.size();
		}
	}


	/**
	 * Gets the list of VM instances.
	 * @return The list of VM instances
	 */
	public LinkedList<VMInstance> getVMList() {
		return this.vmList;
	}


	/**
	 * The public interface for allocating a VM instance. It starts a VM Agent
	 * which will allocate and prepare a VM instance until it is ready to go.
	 */
	public void allocateVM() {
		VMAgent vmAgent = new VMAgent();
		vmAgent.start();
		synchronized (vmAgentList) {
			vmAgentList.add(vmAgent);
		}

		StatisticsManager.instance().addVMAllocationAttempts();
	}


	/**
	 * Adds a VM instance to the resource list.
	 * @param vmInstance The VM instance to be added.
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
	 * Finalizes a VM instance and remove it from the resource list.
	 * @param vmInstance The VM instance to be finalized and removed.
	 */
	public void finalizeVM(VMInstance vm) {
		synchronized (vmList) {
			try {
				CloudBroker broker = CloudBroker.createBroker("ONE");
				broker.finalizeVM(vm);
				vmList.remove(vm);

				logger.debug("VM removed.");

				vm.getProfiler().mark("idleTime");
				vm.getProfiler().mark("deadTime");
				StatisticsManager.instance().addVMProfiler(vm.getId(), vm.getProfiler());

				StatisticsManager.instance().addFinalizedVM();

			} catch (BrokerException e) {
				String msg = String.format("Cannot finalize VM#%d: %s.", vm.getId(), e.getMessage());
				logger.error(msg);
			}
		}
	}


	/**
	 * Removes a VM instance from the resource list.
	 * @param vmInstance The VM instance to be removed.
	 */
	public void removeVM(VMInstance vm) {
		synchronized (vmList) {
			vmList.remove(vm);
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
