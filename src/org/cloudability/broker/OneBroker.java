/**
 * Copyright (C) 2012  Lipu Fei
 */

package org.cloudability.broker;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Scanner;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import org.cloudability.resource.VMInstance;
import org.cloudability.util.BrokerException;
import org.cloudability.util.XmlRpcBroker;

/**
 * Cloud Manipulator for OpenNebula
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class OneBroker extends CloudBroker {

	private String vmTemplatePath;

	protected XmlRpcBroker broker;
	protected XmlRpcClient client;

	/* OpenNebula user session string: <username>:<password> */
	protected String oneUserSession;

	/**
	 * Constructor.
	 * @param serverUrl URL of the XML-RPC server.
	 * @param username Username.
	 * @param password Password.
	 * @throws MalformedURLException
	 */
	public OneBroker(String serverUrl, String username, String password,
				String vmTemplatePath) throws BrokerException {
		super();
		broker = new XmlRpcBroker(serverUrl, username, password);
		client = broker.getClient();
		this.vmTemplatePath = vmTemplatePath;

		/* create OpenNebula user session string */
		this.oneUserSession =
				String.format("%s:%s", username, password);
	}


	/**
	 * Executes an OpenNebula XML-RPC request.
	 * @param cmd The command string.
	 * @param params The parameter list.
	 * @return The OpenNebula response message.
	 */
	protected Object[] execute(String cmd, Vector<Object> params)
			throws BrokerException {
		/* execute the command */
		Object result;
		try {
			result = this.client.execute(cmd, params);
		} catch (XmlRpcException e) {
			/* log and throw exception */
			String error = "XML-RPC client error wile executing request.";
			throw new BrokerException(error);
		}

		/* check the result */
		/* check results */
		if (result != null) {
			Object[] res = (Object[])result;
			Boolean success = (Boolean)res[0];

			if (!success) {
				/* get error message and error code */
				String info = (String)res[1];
				Integer errorCode = (Integer)res[2];

				/* log and throw exception */
				String error = String.format(
						"Error resoponse: ErrorCode=%d, Info=%s",
						errorCode, info);
				throw new BrokerException(error);
			}
		}
		else {
			/* log and throw exception */
			String error = "No response received from XMP-RPC server.";
			throw new BrokerException(error);
		}

		return (Object[])result;
	}

	@Override
	public VMInstance allocateVM() throws BrokerException {
		int vmId = oneAllocateVM(vmTemplatePath);
		VMInstance vm = new VMInstance(vmId);
		return vm;
	}

	@Override
	public void finalizeVM(VMInstance vm) throws BrokerException {
		oneFinalizeVM(vm.getId());
	}

	/**
	 * Allocate a VM instance, corresponding to the OpenNebula EC2 API
	 * vm.one.allocate.
	 * @param templatePath Path to the VM template file.
	 * @return ID of the allocated VM instance.
	 * @throws BrokerException
	 */
	private int oneAllocateVM(String templatePath) throws BrokerException {
		/* read VM template file */
		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(templatePath));
		} catch (FileNotFoundException e) {
			/* log and throw exception. */
			String error = "VM template file not found: " + templatePath + ".";
			throw new BrokerException(error);
		}
		String template = scanner.useDelimiter("\\Z").next();

		/* prepare parameters */
		Vector<Object> params = new Vector<Object>();
		params.add(this.oneUserSession);
		params.add(template);

		/* execute the command */
		Object[] result = this.execute("one.vm.allocate", params);

		return (Integer)result[1];
	}

	/**
	 * Finalize a VM instance, corresponding to the OpenNebula EC2 API
	 * one.vm.action "finalize".
	 * @param vmId ID of the VM to be finalized.
	 */
	private void oneFinalizeVM(int vmId) throws BrokerException {
		/* prepare parameters */
		Vector<Object> params = new Vector<Object>();
		params.add(this.oneUserSession);
		params.add("finalize");
		params.add(vmId);

		/* execute the command */
		this.execute("one.vm.action", params);
	}
}
