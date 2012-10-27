/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.adapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class OneAdapter extends Adapter {

	private XmlRpcClient client;

	/* major information */
	private String serverUrl;
	private String username;
	private String password;
	private String sessionString;
	private String vmTemplate;


	/**
	 * Constructor for OpenNebula adapter. It initialized a client to use
	 * OpenNebula XML-RPC API.
	 * @param serverUrl The URL of the XML-RPC server.
	 * @param username The username.
	 * @param password The password.
	 * @param vmTemplate The VM template.
	 * @throws AdapterException
	 */
	public OneAdapter(String serverUrl, String username, String password,
			String vmTemplate) throws AdapterException {
		this.serverUrl = serverUrl;
		this.username = username;
		this.password = password;
		this.sessionString = String.format("%s:%s", username, password);
		this.vmTemplate = vmTemplate;

		this.initiate();
	}

	/**
	 * Initiates the XML-RPC client.
	 * @throws AdapterException 
	 */
	private void initiate() throws AdapterException {
		/* initialize configuration */
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL(this.serverUrl));
		} catch (MalformedURLException e) {
			String msg = String.format(
					"Invalid URL for XML-RPC server. Message=%s.",
					e.getMessage()
			);
			throw new AdapterException(msg);
		}
		config.setBasicUserName(this.username);
		config.setBasicPassword(this.password);

		/* initialize client */
		this.client = new XmlRpcClient();
		this.client.setConfig(config);
	}

	/**
	 * Executes an OpenNebula XML-RPC request.
	 * @param command The command to execute.
	 * @param parameters The parameters associated.
	 * @return The result object if successful.
	 * @throws AdapterException
	 */
	private Object execute(String command, Vector<Object> parameters)
			throws AdapterException {
		try {
			/* insert session string */
			parameters.add(0, sessionString);
			Object[] result =
					(Object[]) this.client.execute(command, parameters);

			/* check result */
			if (result != null) {
				Boolean successful = (Boolean) result[0];
				if (successful) {
					return result[1];
				}
			}

			String msg = String.format(
					"Execution failed. ErrorCode=%d. Message=%s.",
					(Integer) result[2], (String) result[1]
			);
			throw new AdapterException(msg);
		} catch (XmlRpcException e) {
			String msg = String.format(
					"Execution failed. Message=%s.",
					e.getMessage()
			);
			throw new AdapterException(msg);
		}
	}

	/**
	 * Allocates a VM instance.
	 * @return ID of the allocated VM instance.
	 * @throws AdapterException
	 */
	@Override
	public int allocateVM() throws AdapterException {
		String command = "one.vm.allocate";
		Vector<Object> parameters = new Vector<Object>();
		parameters.add(vmTemplate);

		return (Integer)this.execute(command, parameters);
	}

	/**
	 * Finalizes a VM instance.
	 * @param id ID of the VM instance.
	 * @throws AdapterException
	 */
	@Override
	public int finalizeVM(int id) throws AdapterException {
		String command = "one.vm.action";
		Vector<Object> parameters = new Vector<Object>();
		parameters.add("finalize");
		parameters.add(id);

		return (Integer)this.execute(command, parameters);
	}

	/**
	 * Retrieves information of a VM instance.
	 * @param id ID of the VM instance.
	 * @throws AdapterException
	 */
	@Override
	public String infoVM(int id) throws AdapterException {
		String command = "one.vm.info";
		Vector<Object> parameters = new Vector<Object>();
		parameters.add(id);

		return (String)this.execute(command, parameters);
	}

}
