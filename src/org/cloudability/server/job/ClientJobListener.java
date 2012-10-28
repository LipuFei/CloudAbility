/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.server.job;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;
import org.cloudability.util.CloudLogger;

/**
 * This is a runnable class that listens and accepts client connections.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class ClientJobListener extends Thread {

	private final static Logger logger = CloudLogger.getSystemLogger();

	/* for each try, the listening thread blocks for 1sec */
	private final static int blockingPeriod = 1000;

	private volatile boolean toStop;

	private int port;
	private ServerSocketChannel serverChannel;
	private ExecutorService executorService;

	/**
	 * Constructor.
	 * @param port The port to listen.
	 */
	public ClientJobListener(int port) {
		super();
		this.toStop = false;

		this.port = port;
		this.serverChannel = null;
		this.executorService = Executors.newCachedThreadPool();
	}

	public void setToStop() {
		this.toStop = true;
	}

	/**
	 * It creates a non-blocking socket channel and endlessly listens to client
	 * connections.
	 */
	@Override
	public void run() {
		try {
			String msg = "Initializing Client Request Listener.";
			logger.info(msg);

			/* initialize the server channel as a non-blocking channel */
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.socket().bind(new InetSocketAddress(port));
			this.serverChannel.configureBlocking(false);

			/* register a selector */
			Selector selector = Selector.open();
			this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

			/* start the loop */
			while (!this.toStop) {
				/* continue if no incoming connection */
				if (selector.select(blockingPeriod) == 0)
					continue;

				/* create a thread to handle the incoming channel */
				SocketChannel channel = this.serverChannel.accept();
				if (channel == null)
					continue;

				msg = "Incoming connection accepted, creating a thread.";
				logger.debug(msg);
				this.executorService.execute(new ClientJobHandler(channel));

				/* recreate a selector */
				selector.close();
				selector = Selector.open();
				this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			}
		} catch (IOException e) {
			String msg = "IO Exception occurred: " + e.getMessage();
			logger.error(msg);
		} finally {
			/* finalization */
			String msg = "Finalizing ClientRequestListener...";
			logger.info(msg);

			/* close channel */
			try {
				this.serverChannel.close();
			} catch (IOException e) {
				msg = "IO Exception while closing the server channel: " + e.getMessage();
				logger.error(msg);
			}

			/* cleanup threads */
			msg = "Cleanning up threads...";
			logger.info(msg);
			this.executorService.shutdownNow();
			try {
				this.executorService.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				msg = "Interrupted while waiting for ClientHandlers to shutdown" + e.getMessage();
				logger.error(msg);
			}

			msg = "ClientRequestListener has been finalized.";
			logger.info(msg);
		}
	}

}
