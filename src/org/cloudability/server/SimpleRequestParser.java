/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.server;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.cloudability.scheduling.Job;
import org.cloudability.scheduling.Job.JobStatus;

/**
 * Simple request parser. The syntax is in the form
 * "parameter:value; p2:v2; ...; pn:vn".
 * For more details, please see the documentation.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class SimpleRequestParser extends RequestParser {

	/**
	 * Constructor.
	 */
	public SimpleRequestParser() {
	}

	@Override
	public Job parse(String content) throws RequestSyntaxException {
		String[] pairs = content.trim().split(";");

		HashMap<String, String> parameterMap = new HashMap<String, String>();
		for (int i = 0; i < pairs.length; i++) {
			/* skip empty pair */
			if (pairs[i].trim().isEmpty())
				continue;

			String[] params = pairs[i].trim().split(":");
			/* syntax check */
			if (params.length != 2) {
				/* log and throw exception */
				Logger logger = Logger.getLogger(SimpleRequestParser.class);
				String msg = String.format(
						"Syntax error: %s.", pairs[i].trim());
				logger.error(msg);
				throw new RequestSyntaxException(msg);
			}

			/* add into hash map */
			String parameter = params[0].trim();
			String value = params[1].trim();
			parameterMap.put(parameter, value);
		}

		/* create a job */
		String app = parameterMap.get("app");
		String params = parameterMap.get("params");
		Job job = new Job(Job.generateJobID(), app, params);
		job.setArrivalTime(System.currentTimeMillis());
		job.setStatus(JobStatus.PENDING);

		return job;
	}

}
