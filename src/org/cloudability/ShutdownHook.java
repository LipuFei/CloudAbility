/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability;

import java.io.IOException;

import org.cloudability.analysis.StatisticsManager;
import org.cloudability.resource.ResourceManager;

/**
 * The shutdown hook. Finalizes everything.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class ShutdownHook implements Runnable {

	public ShutdownHook() {
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		/* stop listener */
		CloudAbility.listener.stopListening();
		try {
			CloudAbility.listenerThread.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/* stop scheduler */
		CloudAbility.scheduler.setToStop();
		try {
			CloudAbility.schedulerThread.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/* finalize resource manager */
		ResourceManager.instance().finalize();

		/* save statistics */
		try {
			StatisticsManager.instance().saveToFile("statistics.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
