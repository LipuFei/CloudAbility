/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.adapter;

import org.cloudability.CentralManager;

/**
 * @author Lipu Fei
 * @version 0.1
 *
 */
public abstract class Adapter {

	/**
	 * The factory method for creating a cloud adapter with a given name.
	 * @param name The name of the cloud adapter to be created.
	 * @return The cloud adapter.
	 * @throws AdapterException The initialization of the adapter fails.
	 * @throws RuntimeException The name of the adapter is unknown.
	 */
	public static Adapter createAdapter(String name)
			throws AdapterException, RuntimeException {
		Adapter adapter = null;
		/* OpenNebular adapter for DAS-4 */
		if (name.equals("ONE")) {
			String serverUrl =
				CentralManager.instance().getConfigMap().get("ONE.XMLRPC_URL");
			String username =
				CentralManager.instance().getConfigMap().get("ONE.USERNAME");
			String password =
				CentralManager.instance().getConfigMap().get("ONE.PASSWORD");
			String vmTemplate =
				CentralManager.instance().getConfigMap().get("ONE.VM_TEMPLATE");
			adapter = new OneAdapter(serverUrl, username, password, vmTemplate);
		}
		/* Unknown adapter */
		else {
			String msg = String.format("Unknown adapter name: %s.", name);
			throw new RuntimeException(msg);
		}

		return adapter;
	}


	public abstract int allocateVM() throws AdapterException;
	public abstract int finalizeVM(int id) throws AdapterException;
	public abstract String infoVM(int id) throws AdapterException;

}
