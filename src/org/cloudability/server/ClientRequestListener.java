/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.server;

import java.util.Iterator;
import java.util.LinkedList;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

/**
 * This is a runnable class that listens and accepts client connections.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class ClientRequestListener implements Runnable {

	/* for each try, the listening thread blocks for 1sec */
	private final static int blockingPeriod = 1000;

	private Logger logger;

	private LinkedList<Thread> threadPool;
	private int port;
	private ServerSocketChannel serverChannel;
	private volatile boolean toStop;

	/**
	 * Constructor.
	 * @param port The port to listen.
	 */
	public ClientRequestListener(int port) {
		this.logger = Logger.getLogger(ClientRequestListener.class);

		this.threadPool = new LinkedList<Thread>();
		this.port = port;
		this.serverChannel = null;
		this.toStop = false;
	}

	/**
	 * This method is used to trigger the stop flag.
	 */
	public void stopListening() {
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
			while (true) {
				/* check stop signal */
				if (this.toStop) break;

				/* continue if no incoming connection */
				if (selector.select(blockingPeriod) == 0)
					continue;

				/* create a thread to handle the incoming channel */
				SocketChannel channel = this.serverChannel.accept();
				if (channel == null)
					continue;

				msg = "Incoming connection accepted, creating a thread.";
				logger.debug(msg);
				Thread thread = new Thread(new ClientRequestHandler(channel));
				this.threadPool.add(thread);
				thread.start();

				/* recreate a selector */
				selector.close();
				selector = Selector.open();
				this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			}

			msg = "Finalizing Client Request Listener.";
			logger.info(msg);

			/* close channel */
			selector.close();
			this.serverChannel.close();

			/* cleanup threads */
			msg = "Cleanning up threads.";
			logger.info(msg);
			Iterator<Thread> itr = this.threadPool.iterator();
			while (itr.hasNext()) {
				Thread thread = itr.next();
				thread.join();
			}
			this.threadPool.clear();

			msg = "Client Request Listener has been finalized.";
			logger.info(msg);
		} catch (IOException e) {
			String info = "IOException occurred: " + e.getMessage();
			logger.error(info);
		} catch (InterruptedException e) {
			String info = "Thread interrupted while joining: " + e.getMessage();
			logger.error(info);
		}
	}

}
