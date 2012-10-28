/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.server.job;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.cloudability.scheduling.Job;
import org.cloudability.scheduling.JobState;

/**
 * Simple request parser. The syntax is in the form
 * "parameter:value; p2:v2; ...; pn:vn".
 * For more details, please see the documentation.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class SimpleJobRequestParser extends JobRequestParser {

	/**
	 * Constructor.
	 */
	public SimpleJobRequestParser() {
	}

	@Override
	public Job parse(String content) throws JobRequestSyntaxException {
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
				Logger logger = Logger.getLogger(SimpleJobRequestParser.class);
				String msg = String.format(
						"Syntax error: %s.", pairs[i].trim());
				logger.error(msg);
				throw new JobRequestSyntaxException(msg);
			}

			/* add into hash map */
			String parameter = params[0].trim();
			String value = params[1].trim();
			parameterMap.put(parameter, value);
		}

		/* create a job */
		Job job = new Job(Job.generateJobID(), parameterMap);

		return job;
	}

}
