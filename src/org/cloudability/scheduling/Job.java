/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.cloudability.DataManager;
import org.cloudability.analysis.Profiler;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.VMInstance;
import org.koala.internals.SSHHandler;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;

/**
 * A data class for job that maintains all data related to it. An executable
 * thread can also be created from itself.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class Job implements Runnable {

	/* auto generating job ID */
	private static int maxJobId = 0;

	private Logger logger = Logger.getLogger(Job.class);

	/* every job has a profiler */
	private Profiler jobProfiler = new Profiler();

	/* a signal that indicates if this job should stop */
	private volatile boolean toStop;
	private boolean isStopped;

	/* Statuses of a job */
	public enum JobStatus {
		PENDING, RUNNING, FINISHED, FAILED, STOPPED
	}

	private int id;
	private JobStatus status;
	private VMInstance vmInstance;

	/* parameter map, includes execution related information */
	private HashMap<String, String> parameterMap;

	/* other information needed by other modules */
	private long arrivalTime;
	private long executionTime;
	private long waitTime;

	private int priority;
	private int failure;

	/**
	 * Constructor.
	 * @param id ID of this job.
	 * @param parameterMap The parameter map.
	 */
	public Job(int id, HashMap<String, String> parameterMap) {
		this.arrivalTime = System.currentTimeMillis();
		this.jobProfiler.mark("arrivalTime");

		this.toStop = false;
		this.status = JobStatus.PENDING;

		this.id = id;
		this.vmInstance = null;
		this.parameterMap = parameterMap;

		this.waitTime = 0;
		this.failure = 0;
	}

	/**
	 * Generates a job ID.
	 * @return A job ID.
	 */
	public synchronized static int generateJobID() {
		return maxJobId++;
	}

	/**
	 * Updates a job's wait time.
	 */
	public void updateWaitTime() {
		this.waitTime = System.currentTimeMillis() - this.arrivalTime;
	}

	public long getWaitTime() {
		return this.waitTime;
	}

	public int getFailure() {
		return this.failure;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;

		Job job = (Job)obj;

		if (this.id != job.id) return false;

		return true;
	}

	public Profiler getProfiler() {
		return this.jobProfiler;
	}

	/**
	 * Sets the toStop signal.
	 */
	public void setToStop() {
		this.toStop = true;
	}

	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return this.id;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}
	public JobStatus getStatus() {
		return this.status;
	}

	public void setVMInstance(VMInstance vmInstance) {
		this.vmInstance = vmInstance;
	}
	public VMInstance getVMInstance() {
		return this.vmInstance;
	}

	public void setArrivalTime(long arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public long getArrivalTime() {
		return this.arrivalTime;
	}


	/**
	 * Executes this job.
	 */
	@Override
	public void run() {
		jobProfiler.mark("startTime");

		String msg = "";

		/* preparations */
		jobProfiler.mark("preparationTime");

		/* get IP address of the VM instance and the username to login */
		msg = "Getting VM parameters...";
		logger.debug(msg);
		String vmUsername =
				DataManager.instance().getConfigMap().get("VM.USERNAME");
		String vmIp = this.vmInstance.getIpAddress();

		/* get parameters */
		msg = "Getting application parameters...";
		logger.debug(msg);
		String RemoteDir = parameterMap.get("REMOTE_DIR");
		String appLocal = parameterMap.get("APP.LOCAL");
		String appRemote = parameterMap.get("APP.REMOTE");
		String params = parameterMap.get("APP.PARAMS");
		String inputLocal = parameterMap.get("INPUT.LOCAL");
		String inputRemote = parameterMap.get("INPUT.REMOTE");
		String outputLocal = parameterMap.get("OUTPUT.LOCAL");
		String outputRemote = parameterMap.get("OUTPUT.REMOTE");

		jobProfiler.mark("preparationTime");

		try {
			/* change status to running */
			msg = String.format("Job#%d has been started...", id);
			logger.info(msg);
			this.status = JobStatus.RUNNING;

			/*
			 * STEP #1. upload execution and input files
			 */
			jobProfiler.mark("uploadTime");

			msg = String.format("Job#%d started uploading files...", id);
			logger.info(msg);

			SCPClient scpClient = SSHHandler.getScpClient(vmIp, vmUsername);
			String inputSrcFiles[] = inputLocal.split(",");
			String inputDesFiles[] = inputRemote.split(",");

			String[] tmp = appLocal.split("/");
			String tarballName = tmp[tmp.length - 1];
			scpClient.put(appLocal, tarballName, RemoteDir, "0644");
			scpClient.put(inputSrcFiles, inputDesFiles, RemoteDir, "0644");

			jobProfiler.mark("uploadTime");

			/*
			 * STEP #2. execute the job
			 */
			jobProfiler.mark("tarballExtractionTime");
			/* first uncompress the execution tarball */
			msg = String.format("JOB#%d started extracting the tarball...", id);
			logger.info(msg);

			Session session = SSHHandler.getSession(vmIp, vmUsername);

			String cmd = String.format("tar xzvf %s/%s --directory=%s",
					RemoteDir, tarballName, RemoteDir);
			logger.debug(String.format("JOB#%d: %s", id, cmd));

			InputStream stdout = new StreamGobbler(session.getStdout());
			BufferedReader outRd = new BufferedReader(new InputStreamReader(stdout));
			InputStream stderr = new StreamGobbler(session.getStderr());
			BufferedReader errRd = new BufferedReader(new InputStreamReader(stderr));

			session.execCommand(cmd);
			/* wait until finish */
			while (true) {
				/* check stop signal */
				if (toStop) {
					outRd.close();
					errRd.close();
					session.close();

					msg = String.format("JOB#%d has been stopped.", id);
					logger.info(msg);

					/* set isStop signal and throw exception */
					this.isStopped = true;
					throw new RuntimeException(msg);
				}

				int bitmask = session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
				if ((bitmask & ChannelCondition.EXIT_STATUS) > 0)
					break;

				String line = "";
				if ((bitmask & ChannelCondition.STDOUT_DATA) > 0) {
					if ((line = outRd.readLine()) != null) {
						logger.debug(line);
					}
				}
				if ((bitmask & ChannelCondition.STDERR_DATA) > 0) {
					if ((line = errRd.readLine()) != null) {
						logger.debug(line);
					}
				}
			}
			outRd.close();
			errRd.close();
			session.close();
			/* check status */
			if (session.getExitStatus() != 0) {
				msg = String.format("JOB#%d failed to extract tarball.", id);
				logger.error(msg);
				throw new RuntimeException(msg);
			}

			jobProfiler.mark("tarballExtractionTime");

			/* execute the job */
			jobProfiler.mark("executionTime");

			msg = String.format("JOB#%d started execution...", id);
			logger.info(msg);

			cmd = String.format("%s/%s %s",
					RemoteDir, appRemote, params);
			logger.debug(String.format("JOB#%d: %s", id, cmd));

			session = SSHHandler.getSession(vmIp, vmUsername);

			stdout = new StreamGobbler(session.getStdout());
			outRd = new BufferedReader(new InputStreamReader(stdout));
			stderr = new StreamGobbler(session.getStderr());
			errRd = new BufferedReader(new InputStreamReader(stderr));

			session.execCommand(cmd);
			/* wait until finish */
			while (true) {
				/* check stop signal */
				if (toStop) {
					outRd.close();
					errRd.close();
					session.close();

					msg = String.format("JOB#%d has been stopped.", id);
					logger.info(msg);

					/* set isStop signal and throw exception */
					this.isStopped = true;
					throw new RuntimeException(msg);
				}

				int bitmask = session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
				if ((bitmask & ChannelCondition.EXIT_STATUS) > 0)
					break;

				String line = "";
				if ((bitmask & ChannelCondition.STDOUT_DATA) > 0) {
					if ((line = outRd.readLine()) != null) {
						logger.debug(line);
					}
				}
				if ((bitmask & ChannelCondition.STDERR_DATA) > 0) {
					if ((line = errRd.readLine()) != null) {
						logger.debug(line);
					}
				}
			}
			outRd.close();
			errRd.close();
			session.close();
			/* check status */
			if (session.getExitStatus() != 0) {
				msg = String.format("JOB#%d's execution failed.", id);
				logger.error(msg);
				throw new RuntimeException(msg);
			}

			jobProfiler.mark("executionTime");

			/* STEP #3. download output files */
			jobProfiler.mark("downloadTime");

			msg = String.format("JOB#%d started downloading the output file...", id);
			logger.info(msg);
			logger.debug(RemoteDir + "/" + outputRemote);
			logger.debug(outputLocal);
			scpClient.get(RemoteDir + "/" + outputRemote, outputLocal);

			jobProfiler.mark("downloadTime");

			/* finish */
			msg = String.format("Job#%d is finished.", id);
			logger.info(msg);
			this.status = JobStatus.FINISHED;

			StatisticsManager.instance().addFinishedJob();

		} catch (Exception e) {
			msg = String.format("JOB#%d exception during execution: %s.",
					id, e.getMessage());
			logger.error(msg);

			/* check if it is stopped */
			if (isStopped) {
				this.status = JobStatus.STOPPED;
			}
			else {
				this.status = JobStatus.FAILED;
			}

			/* increase number of failures */
			this.failure++;

			StatisticsManager.instance().addJobsFailure();
		}

		jobProfiler.mark("finishTime");

		/* notify all */
		synchronized (this) {
			this.notifyAll();
		}
	}

}
