/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import org.cloudability.analysis.StatisticsData;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.ResourceManager;
import org.cloudability.scheduling.Job;
import org.cloudability.scheduling.JobMonitor;
import org.cloudability.scheduling.JobQueue;
import org.cloudability.util.CloudConfig;
import org.cloudability.util.CloudConfigException;

/**
 * A singleton class responsible for maintaining all globally shared data,
 * including configurations, job queues, etc.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class DataManager {

	/* the only instance */
	private static DataManager _instance;

	private Logger logger = Logger.getLogger(DataManager.class);

	/* configurations */
	private volatile HashMap<String, String> configMap;

	/* some nasty stuffs */
	private LinkedList<JobMonitor> jobMonitorList;

	/* three job queues */
	private JobQueue pendingJobQueue;
	private JobQueue runningJobQueue;
	private JobQueue finishedJobQueue;

	/**
	 * Constructor.
	 * @throws CloudConfigException 
	 */
	public DataManager(String configFilePath) throws CloudConfigException  {
		/* initialize the configuration map */
		this.configMap = CloudConfig.parseFile(configFilePath);

		this.jobMonitorList = new LinkedList<JobMonitor>();

		this.pendingJobQueue = new JobQueue();
		this.runningJobQueue = new JobQueue();
		this.finishedJobQueue = new JobQueue();
	}

	/**
	 * Initializes the DataManager instance. Must be called first.
	 * @param configFilePath Path to the configuration file.
	 * @throws CloudConfigException
	 */
	public static void initialize(String configFilePath)
				throws CloudConfigException {
		_instance = new DataManager(configFilePath);
	}

	/**
	 * Gets the instance.
	 * @return the DataManager instance.
	 */
	public static DataManager instance() {
		return _instance;
	}

	public HashMap<String, String> getConfigMap() {
		return this.configMap;
	}

	public JobQueue getPendingJobQueue() {
		return this.pendingJobQueue;
	}

	public JobQueue getRunningJobQueue() {
		return this.runningJobQueue;
	}

	public JobQueue getFinishedJobQueue() {
		return this.finishedJobQueue;
	}

	public LinkedList<JobMonitor> getJobMonitorList() {
		return this.jobMonitorList;
	}

	public int getJobMonitorNumber() {
		synchronized (this.jobMonitorList) {
			return this.jobMonitorList.size();
		}
	}

	public void addJobMonitor(JobMonitor jobMonitor) {
		synchronized (this.jobMonitorList) {
			this.jobMonitorList.add(jobMonitor);
		}
	}

	/**
	 * Updates current system status.
	 */
	public void updateSystemStatus() {
		long time = System.currentTimeMillis();
		int pendingJobs = this.getPendingJobQueue().size();
		int runningJobs = this.getJobMonitorNumber();
		int vms = ResourceManager.instance().getVMInstanceNumber();
		int vmAgents = ResourceManager.instance().getVMAgentNumber();

		StatisticsManager.instance().updateMaximumExistingVMs(vms);

		/* update jobs' information */
		LinkedList<Job> jobList = this.getPendingJobQueue().getInstance();
		synchronized (jobList) {
			Iterator<Job> itr = jobList.iterator();
			while (itr.hasNext()) {
				Job job = itr.next();
				job.updateWaitTime();
			}
		}

		String msg = String.format(
				"Jobs in pending queue: %d.", pendingJobs);
		logger.info(msg);
		msg = String.format(
				"JobMonitors running: %d.", runningJobs);
		logger.info(msg);
		msg = String.format(
				"VMInstances in resource pool: %d.", vms);
		logger.info(msg);
		msg = String.format(
				"VMAgents running: %d.", vmAgents);
		logger.info(msg);

		/* save to statistics manager */
		StatisticsData data = new StatisticsData();
		data.add("Time", time);
		data.add("JobsPending", pendingJobs);
		data.add("JobsRunning", runningJobs);
		data.add("VMInstances", vms);
		StatisticsManager.instance().addSystemStatistics(data);
	}

}
