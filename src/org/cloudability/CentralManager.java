/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import org.cloudability.analysis.StatisticsData;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.ResourceManager;
import org.cloudability.scheduling.Job;
import org.cloudability.scheduling.JobState;
import org.cloudability.scheduling.JobQueue;
import org.cloudability.util.CloudConfig;
import org.cloudability.util.CloudConfigException;
import org.cloudability.util.CloudLogger;

/**
 * A singleton class responsible for maintaining all globally shared data,
 * including configurations, job queues, etc.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class CentralManager {

	private static Logger logger = CloudLogger.getSystemLogger();

	/* the only instance */
	private static CentralManager _instance;

	/* configurations */
	private volatile HashMap<String, String> configMap;

	/* some nasty stuffs */
	private ExecutorService jobExecutorService;

	/* three job queues */
	private JobQueue pendingJobQueue;
	private LinkedList<Job> runningJobQueue = new LinkedList<Job>();
	private LinkedList<Job> finishedJobQueue = new LinkedList<Job>();

	/**
	 * Constructor.
	 * @throws CloudConfigException 
	 */
	public CentralManager(String configFilePath) throws CloudConfigException  {
		/* initialize the configuration map */
		this.configMap = CloudConfig.parseFile(configFilePath);

		this.jobExecutorService = Executors.newScheduledThreadPool(10);

		this.pendingJobQueue = new JobQueue();
	}

	/**
	 * Initializes the DataManager instance. Must be called first.
	 * @param configFilePath Path to the configuration file.
	 * @throws CloudConfigException
	 */
	public static void initialize(String configFilePath)
				throws CloudConfigException {
		_instance = new CentralManager(configFilePath);
	}

	/**
	 * This finalizes everything controlled by DataManager, including stopping
	 * jobs in executing and recording unfinished jobs.
	 */
	public static void cleanup() {
		_instance.jobExecutorService.shutdownNow();
		try {
			_instance.jobExecutorService.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			String msg = String.format("Interrupted while waitting for jobs: %s.", e.getMessage());
			logger.error(msg);
		}

		/* record unfinished jobs */
		Iterator<Job> itr = _instance.pendingJobQueue.getInstance().iterator();
		while (itr.hasNext()) {
			Job job = itr.next();
			StatisticsManager.instance().recordUnfinishedJob(job);
			itr.remove();
		}
		itr = _instance.runningJobQueue.iterator();
		while (itr.hasNext()) {
			Job job = itr.next();
			if (job.getState() == JobState.FINISHED)
				StatisticsManager.instance().recordJob(job);
			else
				StatisticsManager.instance().recordUnfinishedJob(job);
			itr.remove();
		}
		itr = _instance.finishedJobQueue.iterator();
		while (itr.hasNext()) {
			Job job = itr.next();
			if (job.getState() == JobState.FINISHED)
				StatisticsManager.instance().recordJob(job);
			else
				StatisticsManager.instance().recordUnfinishedJob(job);
			itr.remove();
		}
	}

	/**
	 * Gets the instance.
	 * @return the DataManager instance.
	 */
	public static CentralManager instance() {
		return _instance;
	}

	public HashMap<String, String> getConfigMap() {
		return this.configMap;
	}


	public JobQueue getPendingJobQueue() {
		return this.pendingJobQueue;
	}


	public LinkedList<Job> getRunningJobQueue() {
		synchronized (this.runningJobQueue) {
			return this.runningJobQueue;
		}
	}

	public int getRunningJobNumber() {
		synchronized (this.runningJobQueue) {
			return this.runningJobQueue.size();
		}
	}

	public void executeJob(Job job) {
		/* execute this job and add it into the running job queue */
		this.jobExecutorService.execute(job);
		synchronized (this.runningJobQueue) {
			this.runningJobQueue.add(job);
		}
	}

	public void removeRunningJob(Job job) {
		synchronized (this.runningJobQueue) {
			this.runningJobQueue.remove(job);
		}
	}


	public LinkedList<Job> getFinishedJobQueue() {
		synchronized (this.finishedJobQueue) {
			return this.finishedJobQueue;
		}
	}

	public int getFinishedJobNumber() {
		synchronized (this.finishedJobQueue) {
			return this.finishedJobQueue.size();
		}
	}

	public void addFinishedJob(Job job) {
		synchronized (this.finishedJobQueue) {
			this.finishedJobQueue.add(job);
		}
	}

	public void removeFinishedJob(Job job) {
		synchronized (this.finishedJobQueue) {
			this.finishedJobQueue.remove(job);
		}
	}

	/**
	 * Updates current system status.
	 */
	public void updateSystemStatus() {
		long time = System.currentTimeMillis();
		int pendingJobs = this.getPendingJobQueue().size();
		int runningJobs = this.getRunningJobNumber();
		int vms = ResourceManager.instance().getResourceNumber();
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

		String msg = "========================================";
		logger.info(msg);
		msg = String.format(
				"Jobs in pending queue: %d.", pendingJobs);
		logger.info(msg);
		msg = String.format(
				"Running job: %d.", runningJobs);
		logger.info(msg);
		msg = String.format(
				"VMInstances in resource pool: %d.", vms);
		logger.info(msg);
		msg = String.format(
				"VMAgents running: %d.", vmAgents);
		logger.info(msg);
		msg = "========================================";
		logger.info(msg);

		/* save to statistics manager */
		StatisticsData data = new StatisticsData();
		data.add("Time", time);
		data.add("JobsPending", new Long(pendingJobs));
		data.add("JobsRunning", new Long(runningJobs));
		data.add("VMInstances", new Long(vms));
		StatisticsManager.instance().addSystemStatistics(data);
	}

}
