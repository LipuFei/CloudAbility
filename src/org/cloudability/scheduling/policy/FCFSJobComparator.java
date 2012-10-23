/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling.policy;

import java.util.Comparator;

import org.cloudability.scheduling.Job;

/**
 * Comparator for FCFS allocation policy. This comparator helps the FCFS policy
 * to sort a job queue by arrival times.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class FCFSJobComparator implements Comparator<Job> {

	@Override
	public int compare(Job job1, Job job2) {
		if (job1.getArrivalTime() < job2.getArrivalTime())
			return -1;
		else if (job1.getArrivalTime() == job2.getArrivalTime())
			return 0;
		else
			return 1;
	}

}
