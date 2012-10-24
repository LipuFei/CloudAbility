/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.cloudability.DataManager;
import org.cloudability.resource.VMInstance;
import org.koala.internals.SSHException;
import org.koala.internals.SSHHandler;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3FileHandle;
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

	/* default buffer size is 8MB */
	private final static int defaultBufferSize = 8 * 1024 * 1024;

	/* auto generating job ID */
	private static int maxJobId = 0;

	/* a signal that indicates if this job should stop */
	private volatile boolean toStop;

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
	 */
	public Job(int id) {
		this.arrivalTime = System.currentTimeMillis();

		this.toStop = false;
		this.status = JobStatus.PENDING;

		this.id = id;
		this.vmInstance = null;
		this.parameterMap = new HashMap<String, String>();
	}

	/**
	 * Constructor.
	 * @param id ID of this job.
	 * @param parameterMap The parameter map.
	 */
	public Job(int id, HashMap<String, String> parameterMap) {
		this.arrivalTime = System.currentTimeMillis();

		this.toStop = false;
		this.status = JobStatus.PENDING;

		this.id = id;
		this.vmInstance = null;
		this.parameterMap = parameterMap;
	}

	/**
	 * Generates a job ID.
	 * @return A job ID.
	 */
	public synchronized static int generateJobID() {
		return maxJobId++;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;

		Job job = (Job)obj;

		if (this.id != job.id) return false;

		return true;
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
		Logger logger = Logger.getLogger(Job.class);
		String msg = "";

		/* change status to running */
		msg = String.format("Job#%d started running.", id);
		logger.debug(msg);
		this.status = JobStatus.RUNNING;

		/* get IP address of the VM instance and the username to login */
		String vmUsername =
				DataManager.instance().getConfigMap().get("VM.USERNAME");;
		String vmIp = this.vmInstance.getIpAddress();

		/* get parameters */
		String RemoteDir = parameterMap.get("REMOTE_DIR");
		String appLocal = parameterMap.get("APP.LOCAL");
		String appRemote = parameterMap.get("APP.REMOTE");
		String params = parameterMap.get("APP.PARAMS");
		String inputLocal = parameterMap.get("INPUT.LOCAL");
		String inputRemote = parameterMap.get("INPUT.REMOTE");
		String outputLocal = parameterMap.get("OUTPUT.LOCAL");
		String outputRemote = parameterMap.get("OUTPUT.REMOTE");

		try {	
			/*
			 * STEP #1. upload execution and input files
			 */
			msg = String.format("Job#%d started uploading files.", id);
			logger.debug(msg);

			SCPClient scpClient = SSHHandler.getScpClient(vmIp, vmUsername);
			String inputSrcFiles[] = inputLocal.split(",");
			String inputDesFiles[] = inputRemote.split(",");

			msg = String.format("Job#%d started uploading files.", id);
			logger.debug(msg);
			String[] tmp = appLocal.split("/");
			String tarballName = tmp[tmp.length - 1];
			scpClient.put(appLocal, tarballName, RemoteDir, "0644");
			scpClient.put(inputSrcFiles, inputDesFiles, RemoteDir, "0644");

			msg = String.format("Job#%d finished uploading files.", id);
			logger.debug(msg);

			/*
			 * STEP #2. execute the job
			 */
			Session session = SSHHandler.getSession(vmIp, vmUsername);

			/* first uncompress the execution tarball */
			msg = "Extracting the tarball...";
			logger.debug(msg);
			String cmd = String.format("tar xzvf %s/%s --directory=%s",
					RemoteDir, tarballName, RemoteDir);

			InputStream stdout = new StreamGobbler(session.getStdout());
			BufferedReader outRd = new BufferedReader(new InputStreamReader(stdout));
			InputStream stderr = new StreamGobbler(session.getStderr());
			BufferedReader errRd = new BufferedReader(new InputStreamReader(stderr));

			session.execCommand(cmd);
			/* wait until finish */
			while (true) {
				int bitmask = session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
				logger.debug("bitmask = " + bitmask);
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
			/* check status */
			if (session.getExitStatus() != 0) {
				msg = "Extracting tarball failed.";
				logger.error(msg);
			}
			outRd.close();
			errRd.close();
			session.close();

			/* execute the job */
			msg = "Executing the job...";
			logger.debug(msg);
			cmd = String.format("%s/%s %s",
					RemoteDir, appRemote, params);
			logger.debug(cmd);

			session = SSHHandler.getSession(vmIp, vmUsername);

			stdout = new StreamGobbler(session.getStdout());
			outRd = new BufferedReader(new InputStreamReader(stdout));
			stderr = new StreamGobbler(session.getStderr());
			errRd = new BufferedReader(new InputStreamReader(stderr));

			session.execCommand(cmd);
			/* wait until finish */
			while (true) {
				int bitmask = session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
				logger.debug("bitmask = " + bitmask);
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
			/* check status */
			if (session.getExitStatus() != 0) {
				msg = "Extracting tarball failed.";
				logger.error(msg);
			}
			outRd.close();
			errRd.close();
			session.close();

			/* STEP #3. download output files */
			msg = "Downloading the output file...";
			logger.debug(msg);
			logger.debug(RemoteDir + "/" + outputRemote);
			logger.debug(outputLocal);
			scpClient.get(RemoteDir + "/" + outputRemote, outputLocal);

			/* finish */
			msg = String.format("Job#%d is finished.", id);
			logger.debug(msg);
			this.status = JobStatus.FINISHED;

		} catch (SSHException e) {
			msg = String.format("SSH Exception during execution: %s.",
					e.getMessage());
			logger.error(msg);

			/* set to failed */
			this.status = JobStatus.FAILED;
		} catch (IOException e) {
			msg = String.format("IO Exception during execution: %s.",
					e.getMessage());
			logger.error(msg);

			/* set to failed */
			this.status = JobStatus.FAILED;
		} catch (Exception e) {
			msg = String.format("Other Exception during execution: %s.",
					e.getMessage());
			logger.error(msg);

			/* set to failed */
			this.status = JobStatus.FAILED;
		}

		/* notify all */
		synchronized (this) {
			this.notifyAll();
		}
	}

}
