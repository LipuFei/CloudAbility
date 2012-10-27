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
public class Recorder {

	/* the hash map that maintains the records */
	private HashMap<String, Long> recordMap = new HashMap<String, Long>();


	public Recorder() {
	}

	/**
	 * Records a value with a given name. The old value associated with this
	 * name will be overwritten.
	 * @param name The name of this record.
	 * @param value The value associated to this key.
	 */
	public void record(String name, Long value) {
		this.recordMap.put(name, value);
	}

	/**
	 * Increases a counter with a given name. If this counter doesn't exist, it
	 * initiates this counter with 0.
	 * @param name The name of this counter.
	 */
	public void count(String name) {
		long value = 0L;
		if (this.recordMap.containsKey(name)) {
			value += this.recordMap.remove(name);
		}
		this.recordMap.put(name, value);
	}

	/**
	 * Retrieves a value with a given name.
	 * @param name The name of the value to retrieve.
	 * @return The value if present, null if not.
	 */
	public long retrieve(String name) {
		return this.recordMap.get(name);
	}

}
