/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.server.job;

import org.cloudability.scheduling.Job;

/**
 * The parser for parsing client requests.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public abstract class JobRequestParser {

	/**
	 * Parses a request content.
	 * @param content The request content.
	 * @return The parsed object.
	 */
	public abstract Job parse(String content) throws JobRequestSyntaxException;

}
