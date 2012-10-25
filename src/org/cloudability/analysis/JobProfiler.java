/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.analysis;

import java.util.HashMap;

/**
 * A profiler for evaluating the job's performance.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class JobProfiler {

	private HashMap<String, Long> markMap;
	private HashMap<String, Long> statisticsMap;

	/**
	 * Constructor.
	 */
	public JobProfiler() {
		this.markMap = new HashMap<String, Long>();
		this.statisticsMap = new HashMap<String, Long>();
	}

	/**
	 * Gets the statistics map.
	 * @return The statistics map.
	 */
	public HashMap<String, Long> getStatisticsMap() {
		return this.statisticsMap;
	}

	public long getMark(String key) {
		return this.markMap.get(key);
	}

	/**
	 * Gets a value from the statistics map with a given key.
	 * @return The period of the given key if exists, null if not.
	 */
	public long getPeriod(String key) {
		return this.statisticsMap.get(key);
	}

	/**
	 * Clears all marks.
	 */
	public void clearMarks() {
		this.markMap.clear();
	}

	/**
	 * Clears all statistics.
	 */
	public void clearStatistics() {
		this.statisticsMap.clear();
	}

	/**
	 * Clears all.
	 */
	public void clearAll() {
		clearMarks();
		clearStatistics();
	}

	/**
	 * Set a mark with a given name.
	 * @param key The name of this mark.
	 */
	public void mark(String key) {
		long thisTime = System.currentTimeMillis();

		/*
		 * if the mark exists, remove it and aggregate this period into the
		 * statistics map.
		 */
		if (this.markMap.containsKey(key)) {
			long start = this.markMap.remove(key);
			long period = thisTime - start;
			if (this.statisticsMap.containsKey(key)) {
				period += this.statisticsMap.remove(key);
			}
			this.statisticsMap.put(key, period);
		}
		/*
		 * if the mark doesn't exist, just add it into the mark map.
		 */
		else {
			this.markMap.put(key, thisTime);
		}
	}

}