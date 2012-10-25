/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.analysis;

import java.util.HashMap;

/**
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class StatisticsData {

	private HashMap<String, Long> dataMap;

	public StatisticsData() {
		this.dataMap = new HashMap<String, Long>();
	}

	public void add(String name, long value) {
		this.dataMap.put(name, value);
	}

	public long get(String name) {
		return this.dataMap.get(name);
	}

}
