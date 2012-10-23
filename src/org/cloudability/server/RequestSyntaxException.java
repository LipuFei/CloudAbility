/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.server;

/**
 * Request syntax exception.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class RequestSyntaxException extends Exception {

	private static final long serialVersionUID = -4302035670969911975L;

	/**
	 * 
	 */
	public RequestSyntaxException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public RequestSyntaxException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public RequestSyntaxException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public RequestSyntaxException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

}
