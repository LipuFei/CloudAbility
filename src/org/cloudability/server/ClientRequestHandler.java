/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.cloudability.DataManager;
import org.cloudability.analysis.StatisticsManager;
import org.cloudability.scheduling.Job;

/**
 * This is a runnable class that handles a single client request.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class ClientRequestHandler implements Runnable {

	private final static String defaultCharset = "UTF-8";
	private final static int defaultBufferSize = 128 * 1024;

	private Logger logger;

	private SocketChannel channel;
	private ByteBuffer buffer;
	private Charset charset;

	/**
	 * Constructor.
	 * @param channel The incoming client channel.
	 */
	public ClientRequestHandler(SocketChannel channel) {
		this.logger = Logger.getLogger(ClientRequestHandler.class);
		this.channel = channel;
		this.buffer = ByteBuffer.allocate(defaultBufferSize);
		this.charset = Charset.forName(defaultCharset);
	}

	/**
	 * This method receives the incoming job request description file, parses
	 * it, and then processes it.
	 */
	@Override
	public void run() {
		String content = "";
		buffer.clear();
		try {
			String msg = "Client start reading data.";
			logger.debug(msg);

			/* read the incoming data */
			while (true) {
				int readBytes = channel.read(buffer);

				if (readBytes == -1)
					break;
				else if (readBytes == 0)
					continue;

				if (buffer.remaining() == 0) {
					content += new String(buffer.array());
					//content += new String(charset.decode(buffer).array());
					buffer.clear();
				}
			}
			content += new String(buffer.array());
			//content += new String(charset.decode(buffer).array());

			/* close channel */
			channel.close();

			/* parse the file */
			msg = "Parsing client request.";
			logger.debug(msg);
			logger.debug("===== CONTENT BEGIN =====");
			logger.debug(content);
			logger.debug("===== CONTENT END =====");
			RequestParser parser = new SimpleRequestParser();
			Job job = parser.parse(content);

			/* put job into the pending queue */
			DataManager.instance().getPendingJobQueue().addJob(job);

			StatisticsManager.instance().addAcceptedJob();

		} catch (IOException e) {
			String msg = String.format("IO exception: %s.", e.getMessage());
			logger.error(msg);
		} catch (RequestSyntaxException e) {
			String msg = String.format(
					"Request syntax exception: %s.",
					e.getMessage());
			logger.error(msg);
		}
	}

}
