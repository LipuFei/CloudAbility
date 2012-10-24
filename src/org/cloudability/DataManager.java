package org.cloudability;

import java.util.HashMap;
import java.util.LinkedList;

import org.cloudability.resource.VMAgent;
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

	/* configurations */
	private volatile HashMap<String, String> configMap;

	/* some nasty stuffs */
	private LinkedList<Thread> jobMonitorThreadList;

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

		this.jobMonitorThreadList = new LinkedList<Thread>();

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

	public void addJobMonitorThread(Thread jobMonitorThread) {
		synchronized (jobMonitorThreadList) {
			jobMonitorThreadList.add(jobMonitorThread);
		}
	}

}
