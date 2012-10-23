/**
 * Copyright (C) 2012  Lipu Fei
 */

package org.cloudability.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * A utility class for creating an XML-RPC client.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class XmlRpcBroker {

	protected String serverUrl;
	protected String username;
	protected String password;

	protected XmlRpcClient client;

	/**
	 * Constructor.
	 * @param serverUrl serverUrl URL of the XML-RPC server.
	 * @param username Username.
	 * @param password Password.
	 */
	public XmlRpcBroker(String serverUrl, String username, String password)
			throws BrokerException {
		this.serverUrl = serverUrl;
		this.username = username;
		this.password = password;

		/* initialize configuration. */
		System.out.println("Initializing client configuration");
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL(this.serverUrl));
		} catch (MalformedURLException e) {
			/* log and throw exception */
			String error = String.format("Invalid URL: %s.", this.serverUrl);
			throw new BrokerException(error);
		}
		config.setBasicUserName(this.username);
		config.setBasicPassword(this.password);

		/* initialize client. */
		System.out.println("Initializing client");
		this.client = new XmlRpcClient();
		this.client.setConfig(config);
	}

	/**
	 * Gets the XML-RPC client.
	 * @return The XML-RPC client.
	 */
	public XmlRpcClient getClient() {
		return this.client;
	}

}
