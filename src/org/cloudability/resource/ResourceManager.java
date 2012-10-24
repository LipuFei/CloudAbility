/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.cloudability.broker.CloudBroker;
import org.cloudability.resource.policy.Provisioner;
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

	/* provisioning policy */
	private Provisioner provisioner;
	private Thread provisionerThread;

	/**
	 * Constructor.
	 * @throws CloudConfigException
	 */
	public ResourceManager() throws CloudConfigException {
		this.logger = Logger.getLogger(ResourceManager.class);

		this.vmList = new LinkedList<VMInstance>();
	}

	/**
	 * Initialization.
	 * @throws CloudConfigException
	 */
	public static void initialize() throws CloudConfigException {
		_instance = new ResourceManager();

		/* initialize provisioning policy */
		//_instance.provisioner = new StaticProvisioner();
		/* start a thread for the provisioner */
		//_instance.provisionerThread = new Thread(_instance.provisioner); 

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
		//try {
			/* stop the provisioning thread */
			String msg = "Stopping provisioner thread.";
			_instance.logger.info(msg);

			//_instance.provisioner.setStop();
			//_instance.provisionerThread.join();

			msg = "Provisioner thread has been stopped.";
			_instance.logger.info(msg);

			/* release all VMs */
			/*
			CloudBroker borker = CloudBroker.createBroker("ONE");
			Iterator<VMInstance> itr = _instance.vmList.iterator();
			while (itr.hasNext()) {
				VMInstance vm = itr.next();
				borker.finalizeVM(vm);
			}
			*/

			_instance.vmList.clear();
		/*
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
		*/
	}

	/**
	 * Gets the list of VM instances.
	 * @return The list of VM instances
	 */
	public LinkedList<VMInstance> getVMList() {
		return this.vmList;
	}

	/**
	 * Add a VM instance to the resource list.
	 * @param vmInstance The VM instance to be added.
	 */
	public void addVM(VMInstance vm) {
		synchronized (vmList) {
			vmList.add(vm);
			String info = String.format(
					"VM#%d has been added to the resource list.",
					vm.getId());
			logger.debug(info);
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
