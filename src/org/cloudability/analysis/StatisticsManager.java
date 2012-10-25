/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * 
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class StatisticsManager {

	/* the only instance */
	private final static StatisticsManager _instance = new StatisticsManager();

	/* maintains statistics of all finished jobs */
	private HashMap<Integer, StatisticsData> jobStatisticsMap;

	/* maintains system performance over time */
	private LinkedList<StatisticsData> systemPerformanceList;

	/* maintains system statistics,
	 * such as #jobs-finished, #jobs-failed, etc.
	 */
	private HashMap<String, Long> systemStatisticsMap;


	public StatisticsManager() {
		this.jobStatisticsMap = new HashMap<Integer, StatisticsData>();
		this.systemPerformanceList = new LinkedList<StatisticsData>();
		this.systemStatisticsMap = new HashMap<String, Long>();

		this.initializeSystemStatisticsMap();
	}

	private void initializeSystemStatisticsMap() {
		long value = 0;
		this.systemStatisticsMap.put("JobsAccepted", value);
		this.systemStatisticsMap.put("JobsFinished", value);
		this.systemStatisticsMap.put("JobsFailed", value);

		this.systemStatisticsMap.put("VMsAllocated", value);
		this.systemStatisticsMap.put("VMsFinalized", value);
		this.systemStatisticsMap.put("MaximumVMsExisting", value);

		this.systemStatisticsMap.put("VMAllocationAttempts", value);
		this.systemStatisticsMap.put("VMAllocationFailures", value);
	}

	/**
	 * Public interface for getting the only instance.
	 * @return The StatisticsManager instance.
	 */
	public static StatisticsManager instance() {
		return _instance;
	}

	public void addJobStatistics(int jobId, StatisticsData data) {
		synchronized (this.jobStatisticsMap) {
			this.jobStatisticsMap.put(jobId, data);
		}
	}

	public void addSystemStatistics(StatisticsData data) {
		synchronized (this.systemPerformanceList) {
			this.systemPerformanceList.add(data);
		}
	}

	public void addAcceptedJob() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("JobsAccepted");
			this.systemStatisticsMap.put("JobsAccepted", value + 1);
		}
	}

	public void addFinishedJob() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("JobsFinished");
			this.systemStatisticsMap.put("JobsFinished", value + 1);
		}
	}

	public void addFailedJob() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("JobsFailed");
			this.systemStatisticsMap.put("JobsFailed", value + 1);
		}
	}

	public void addAllocatedVM() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("VMsAllocated");
			this.systemStatisticsMap.put("VMsAllocated", value + 1);
		}
	}

	public void addFinalizedVM() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("VMsFinalized");
			this.systemStatisticsMap.put("VMsFinalized", value + 1);
		}
	}

	public void addVMAllocationAttempts() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("VMAllocationAttempts");
			this.systemStatisticsMap.put("VMAllocationAttempts", value + 1);
		}
	}

	public void addVMAllocationFailures() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("VMAllocationFailures");
			this.systemStatisticsMap.put("VMAllocationFailures", value + 1);
		}
	}

	/**
	 * Saves statistics to a specified file.
	 * @param outFilePath
	 * @throws IOException
	 */
	public void saveToFile(String outFilePath) throws IOException {
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(outFilePath));

		String content = "";

		writer.write("Statistics\n====================\n");

		/* system statistics */
		writer.write("\nSystem Statistics\n====================\n");
		content = String.format("#Jobs accepted: %d\n", systemStatisticsMap.get("JobsAccepted"));
		content += String.format("#Jobs finished: %d\n", systemStatisticsMap.get("JobsFinished"));
		content += String.format("#Jobs failed: %d\n", systemStatisticsMap.get("JobsFailed"));

		content += String.format("#VMs allocated: %d\n", systemStatisticsMap.get("VMsAllocated"));
		content += String.format("#VMs finalized: %d\n", systemStatisticsMap.get("VMsFinalized"));
		content += String.format("#Maximum existing VMs: %d\n", systemStatisticsMap.get("MaximumExistingVMs"));

		content += String.format("#VM allocation attempts: %d\n", systemStatisticsMap.get("VMAllocationAttempts"));
		content += String.format("#VM allocation failures: %d\n", systemStatisticsMap.get("VMAllocationFailures"));
		writer.write(content);

		/* system performance over time */
		writer.write("\nSystem Statistics\n====================\n");
		writer.write("Time #JobsPending #JobsRunning #VMInstances\n");
		Iterator<StatisticsData> itr1 = systemPerformanceList.iterator();
		while (itr1.hasNext()) {
			StatisticsData data = itr1.next();
			content = String.format("%d %d %d %d\n",
					data.get("Time"),
					data.get("JobsPending"),
					data.get("JobsRunning"),
					data.get("VMInstances"));
		}

		/* overall job statistics */
		writer.write("\nJob Statistics\n====================\n");
		writer.write("Overall\n");
		long makespan = 0;
		long waitTime = 0;
		long runningTime = 0;
		long preparationTime = 0;
		long uploadTime = 0;
		long tarballExtractionTime = 0;
		long executionTime = 0;
		long downloadTime = 0;
		long totalNumber = 0;
		Iterator<Entry<Integer, StatisticsData>> itr2 = jobStatisticsMap.entrySet().iterator();
		while (itr2.hasNext()) {
			Entry<Integer, StatisticsData> entry = itr2.next();
			StatisticsData data = entry.getValue();

			makespan += data.get("makespan");
			waitTime += data.get("waitTime");
			runningTime += data.get("runningTime");
			preparationTime += data.get("preparationTime");
			uploadTime += data.get("uploadTime");
			tarballExtractionTime += data.get("tarballExtractionTime");
			executionTime += data.get("executionTime");
			downloadTime += data.get("downloadTime");

			totalNumber++;
		}
		if (totalNumber != 0) {
			content = String.format("makespan=%.3f sec\n", (double)makespan / totalNumber / 1000);
			content += String.format("waitTime=%.3f sec\n", (double)waitTime / totalNumber / 1000);
			content += String.format("runningTime=%.3f sec\n", (double)runningTime / totalNumber / 1000);
			content += String.format("preparationTime=%.3f sec\n", (double)preparationTime / totalNumber / 1000);
			content += String.format("uploadTime=%.3f sec\n", (double)uploadTime / totalNumber / 1000);
			content += String.format("tarballExtractionTime=%.3f sec\n", (double)tarballExtractionTime / totalNumber / 1000);
			content += String.format("executionTime=%.3f sec\n", (double)executionTime / totalNumber / 1000);
			content += String.format("downloadTime=%.3f sec\n", (double)downloadTime / totalNumber / 1000);
		}
		else {
			content = "null\n";
		}
		writer.write(content);

		/* detailed job statistics */

		/* flush and close */
		writer.flush();
		writer.close();
	}

}
