/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability;

import java.io.IOException;

import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.ResourceManager;

import org.apache.log4j.Logger;

/**
 * The shutdown hook. Finalizes everything.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class ShutdownHook implements Runnable {

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Logger logger = Logger.getLogger(ShutdownHook.class);
		/* stop listener */
		logger.info("Stopping listener...");
		CloudAbility.listenerThread.setToStop();
		try {
			CloudAbility.listenerThread.join();
		} catch (InterruptedException e) {
			String msg = String.format("Interrupted while waiting for Listener to stop: %s.", e.getMessage());
			logger.info(msg);
		}

		/* stop scheduler */
		logger.info("Stopping scheduler...");
		CloudAbility.schedulerThread.interrupt();
		try {
			CloudAbility.schedulerThread.join();
		} catch (InterruptedException e) {
			String msg = String.format("Interrupted while waiting for Scheduler to stop: %s.", e.getMessage());
			logger.info(msg);
		}

		/* finalize resource manager */
		logger.info("Finalizing resource manager...");
		ResourceManager.cleanup();

		/* save statistics */
		logger.info("Saving statistics...");
		try {
			StatisticsManager.instance().saveToFile("statistics.txt");
		} catch (IOException e) {
			String msg = String.format("IO Exception while saving statistics: %s.", e.getMessage());
			logger.info(msg);
		}
	}

}
