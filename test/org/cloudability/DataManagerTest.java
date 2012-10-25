package org.cloudability;

import java.util.HashMap;

import org.cloudability.util.CloudConfig;
import org.cloudability.util.CloudConfigException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DataManagerTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void getConfigMapTest() {
		try {
			HashMap<String, String> configMap = CloudConfig.parseFile("config/cloud.test.config");
			Assert.assertEquals(configMap.get("VM.USERNAME"), "root");
		} catch (CloudConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
