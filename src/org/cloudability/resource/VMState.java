/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

/**
 * States of a VM instance.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public enum VMState {
	PENDING, BOOTING, RUNNING, SHUTDOWN, UNKNOWN,
	INVALID	/* This state is for special purpose */
}
