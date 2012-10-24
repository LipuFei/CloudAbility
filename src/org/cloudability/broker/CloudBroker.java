/**
 * 
 */
package org.cloudability.broker;

import org.apache.log4j.Logger;
import org.cloudability.DataManager;
import org.cloudability.util.BrokerException;
import org.cloudability.resource.VMInstance;

/**
 * An abstract class for cloud broker. It is also a factory that creates cloud
 * brokers.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public abstract class CloudBroker {

	protected static Logger logger = Logger.getLogger(CloudBroker.class);

	/**
	 * Constructor.
	 */
	public CloudBroker() {
	}

	/**
	 * Creates a cloud broker.
	 * @return A cloud broker.
	 * @throws BrokerException
	 */
	public static CloudBroker createBroker(String name) throws BrokerException {
		/* DAS-4 OpenNebula Broker */
		if (name.equals("ONE")) {
			String serverUrl =
				DataManager.instance().getConfigMap().get("ONE.XMLRPC_URL");
			String username =
				DataManager.instance().getConfigMap().get("ONE.USERNAME");
			String password =
				DataManager.instance().getConfigMap().get("ONE.PASSWORD");
			String vmTemplatePath =
				DataManager.instance().getConfigMap().get("ONE.VM_TEMPLATE");
			return new OneBroker(serverUrl, username, password, vmTemplatePath);
		}
		/* Unknown broker, log and throw an exception */
		else {
			String msg = String.format("Unknown broker name: %s.", name);
			logger.error(msg);
			throw new BrokerException(msg);
		}
	}

	/**
	 * Allocates a VM instance.
	 * @return an allocated VM instance if succeeded, null otherwise.
	 * @throws BrokerException
	 */
	public abstract VMInstance allocateVM() throws BrokerException;

	/**
	 * Finalizes a VM instance.
	 * @throws BrokerException
	 */
	public abstract void finalizeVM(VMInstance vm) throws BrokerException;

	public abstract void updateInfo(VMInstance vm) throws BrokerException;

}
