package org.cloudability.broker;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.cloudability.util.BrokerException;
import org.cloudability.util.CloudConfig;
import org.cloudability.util.CloudConfigException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class OneBrokerTest {
	static HashMap<String, String> configMap;
	static OneBroker oneBroker ;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String configFilePath = "config/cloud.test.config";
		configMap = null;
		try {
			configMap = CloudConfig.parseFile(configFilePath);
		} catch (CloudConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		oneBroker = null;
		try {
			oneBroker = new OneBroker(configMap.get("ONE.XMLRPC_URL"),
					configMap.get("ONE.USERNAME"),
					configMap.get("ONE.PASSWORD"),
					configMap.get("ONE.VM_TEMPLATE"));

		} catch (BrokerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Assert.assertNotNull(oneBroker);

	}
	
	@Test
	public void AllocateVMTest()
	{
		try {
			oneBroker.allocateVM();
		} catch (BrokerException e) {
			fail();
			e.printStackTrace();
		}
	}
}
