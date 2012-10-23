/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.scheduling;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * A synchronized queue for scheduling jobs.
 * @author Lipu Fei
 *
 */
public class JobQueue {

	private LinkedList<Job> queue;

	public JobQueue() {
		this.queue = new LinkedList<Job>();
	}

	/**
	 * Gets the queue instance.
	 * @return The queue instance.
	 */
	public LinkedList<Job> getInstance() {
		return queue;
	}

	/**
	 * Checks if the queue is empty.
	 * @return true if empty, false if not.
	 */
	public boolean isEmpty() {
		boolean empty = false;
		synchronized (queue) {
			empty = queue.isEmpty();
		}
		return empty;
	}

	/**
	 * Adds a job to the end of the queue.
	 * @param job The job to be added.
	 */
	public void addJob(Job job) {
		synchronized (queue) {
			queue.add(job);
		}
		synchronized (this) {
			/* notify all the waiting threads */
			this.notifyAll();
		}
	}

	/**
	 * Removes and gets the first job in the queue.
	 * @return job The first job in the queue if not empty, otherwise null.
	 */
	public Job popJob() {
		Job job = null;
		synchronized (queue) {
			if (!queue.isEmpty())
				job = queue.pop();
		}
		return job;
	}

	/**
	 * Sorts this queue with a given comparator.
	 * @param comparator The comparator for sorting.
	 */
	public void sort(Comparator<Job> comparator) {
		synchronized (queue) {
			Collections.sort(queue, comparator);
		}
	}

}
