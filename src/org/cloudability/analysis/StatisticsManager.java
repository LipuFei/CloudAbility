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
 * Responsible for collecting system performance results and print them to a
 * file.
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

	/* for calculating VM preparation time */
	private LinkedList<Long> vmPreparationTimeList;

	/* maintains system statistics,
	 * such as #jobs-finished, #jobs-failed, etc.
	 */
	private HashMap<String, Long> systemStatisticsMap;


	public StatisticsManager() {
		this.jobStatisticsMap = new HashMap<Integer, StatisticsData>();
		this.systemPerformanceList = new LinkedList<StatisticsData>();
		this.systemStatisticsMap = new HashMap<String, Long>();
		this.vmPreparationTimeList = new LinkedList<Long>();

		this.initializeSystemStatisticsMap();
	}

	private void initializeSystemStatisticsMap() {
		long value = 0;
		this.systemStatisticsMap.put("JobsAccepted", value);
		this.systemStatisticsMap.put("JobsFinished", value);
		this.systemStatisticsMap.put("JobsFailures", value);

		this.systemStatisticsMap.put("VMsAllocated", value);
		this.systemStatisticsMap.put("VMsFinalized", value);
		this.systemStatisticsMap.put("MaximumExistingVMs", value);

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

	public void addJobsFailure() {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("JobsFailures");
			this.systemStatisticsMap.put("JobsFailures", value + 1);
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

	public void updateMaximumExistingVMs(long vms) {
		synchronized (this.systemStatisticsMap) {
			long value = this.systemStatisticsMap.remove("MaximumExistingVMs");
			value = value > vms ? value : vms;
			this.systemStatisticsMap.put("MaximumExistingVMs", value);
		}
	}

	public void addVMPreparationTime(long time) {
		synchronized (this.vmPreparationTimeList) {
			this.vmPreparationTimeList.add(time);
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

		writer.write("====================\nStatistics\n====================\n");

		/* system statistics */
		writer.write("\n====================\nSystem Statistics\n====================\n");
		content = String.format("Jobs accepted: %d\n", systemStatisticsMap.get("JobsAccepted"));
		content += String.format("Jobs finished: %d\n", systemStatisticsMap.get("JobsFinished"));
		content += String.format("Jobs failures: %d\n", systemStatisticsMap.get("JobsFailures"));

		content += String.format("VMs allocated: %d\n", systemStatisticsMap.get("VMsAllocated"));
		content += String.format("VMs finalized: %d\n", systemStatisticsMap.get("VMsFinalized"));
		content += String.format("Maximum existing VMs: %d\n", systemStatisticsMap.get("MaximumExistingVMs"));

		content += String.format("VM allocation attempts: %d\n", systemStatisticsMap.get("VMAllocationAttempts"));
		content += String.format("VM allocation failures: %d\n", systemStatisticsMap.get("VMAllocationFailures"));

		long[] vmPreparationTime = createMetric();
		Iterator<Long> itr1 = this.vmPreparationTimeList.iterator();
		while (itr1.hasNext()) {
			updateMetric(vmPreparationTime, itr1.next());
		}
		if (this.vmPreparationTimeList.size() > 0) {
			content += String.format(
					"VM preparation time: %s sec\n",
					formatMetric(vmPreparationTime, this.vmPreparationTimeList.size()));
		}
		else {
			content += "VM preparation time: null\n";
		}

		writer.write(content);

		/* overall job statistics */
		writer.write("\n====================\nOverall Job Statistics\n====================\n");

		long[] makespan = createMetric();
		long[] waitTime = createMetric();
		long[] runningTime = createMetric();
		long[] preparationTime = createMetric();
		long[] uploadTime = createMetric();
		long[] tarballExtractionTime = createMetric();
		long[] executionTime = createMetric();
		long[] downloadTime = createMetric();

		long totalNumber = 0;

		Iterator<Entry<Integer, StatisticsData>> itr2 = jobStatisticsMap.entrySet().iterator();
		while (itr2.hasNext()) {
			Entry<Integer, StatisticsData> entry = itr2.next();
			StatisticsData data = entry.getValue();

			updateMetric(makespan, data.get("makespan"));
			updateMetric(waitTime, data.get("waitTime"));
			updateMetric(runningTime, data.get("runningTime"));
			updateMetric(preparationTime, data.get("preparationTime"));
			updateMetric(uploadTime, data.get("uploadTime"));
			updateMetric(tarballExtractionTime, data.get("tarballExtractionTime"));
			updateMetric(executionTime, data.get("executionTime"));
			updateMetric(downloadTime, data.get("downloadTime"));

			totalNumber++;
		}
		if (totalNumber != 0) {
			content = String.format("makespan=%s sec\n", formatMetric(makespan, totalNumber));
			content += String.format("waitTime=%s sec\n", formatMetric(waitTime, totalNumber));
			content += String.format("runningTime=%s sec\n", formatMetric(runningTime, totalNumber));
			content += String.format("preparationTime=%s sec\n", formatMetric(preparationTime, totalNumber));
			content += String.format("uploadTime=%s sec\n", formatMetric(uploadTime, totalNumber));
			content += String.format("tarballExtractionTime=%s sec\n", formatMetric(tarballExtractionTime, totalNumber));
			content += String.format("executionTime=%s sec\n", formatMetric(executionTime, totalNumber));
			content += String.format("downloadTime=%s sec\n", formatMetric(downloadTime, totalNumber));
		}
		else {
			content = "null\n";
		}
		writer.write(content);

		/* detailed job statistics */
		writer.write("\n====================\nDetail Job Statistics\n====================\n");
		writer.write("#jobId arrivalTime makespan waitTime runningTime preparationTime uploadTime tarballExtractionTime executionTime downloadTime\n");
		itr2 = jobStatisticsMap.entrySet().iterator();
		while (itr2.hasNext()) {
			Entry<Integer, StatisticsData> entry = itr2.next();
			StatisticsData data = entry.getValue();

			content = String.format("%d %d %.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f\n",
					entry.getKey(),
					data.get("arrivalTime"),
					(double)data.get("makespan") / 1000,
					(double)data.get("waitTime") / 1000,
					(double)data.get("runningTime") / 1000,
					(double)data.get("preparationTime") / 1000,
					(double)data.get("uploadTime") / 1000,
					(double)data.get("tarballExtractionTime") / 1000,
					(double)data.get("executionTime") / 1000,
					(double)data.get("downloadTime") / 1000
			);

			writer.write(content);
		}

		/* system performance over time */
		writer.write("\n====================\nSystem Performance Over Time\n====================\n");
		writer.write("#Timestamp #JobsPending #JobsRunning #VMInstances\n");
		Iterator<StatisticsData> itr3 = systemPerformanceList.iterator();
		while (itr3.hasNext()) {
			StatisticsData data = itr3.next();
			content = String.format("%d %d %d %d\n",
					data.get("Time"),
					data.get("JobsPending"),
					data.get("JobsRunning"),
					data.get("VMInstances"));

			writer.write(content);
		}

		/* flush and close */
		writer.flush();
		writer.close();
	}

	private long[] createMetric() {
		long[] metric = new long[3];
		metric[0] = Long.MAX_VALUE;
		metric[1] = 0;
		metric[2] = Long.MIN_VALUE;
		return metric;
	}

	private void updateMetric(long[] metric, long value) {
		metric[0] = metric[0] < value ? metric[0] : value;
		metric[1] += value;
		metric[2] = metric[2] > value ? metric[2] : value;
	}

	private String formatMetric(long[] metric, long total) {
		return String.format("[%.3f %.3f %.3f]",
				(double)metric[0] / 1000,
				(double)metric[1] / total / 1000,
				(double)metric[2] / 1000);
	}

}
