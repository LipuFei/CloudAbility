/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.resource;

/**
 * The VM instance states in OpenNebula.
 * @author Lipu Fei
 * @version 0.1
 *
 */
public enum OneVMState {

	PENDING			(0),
	PROLOG			(1),
	BOOT			(2),
	RUNNING			(3),
	MIGRATE			(4),
	SAVE_STOP		(5),
	SAVE_SUSPEND	(6),
	SAVE_MIGRATE	(7),
	PROLOG_MIGRATE	(8),
	PROLOG_RESUME	(9),
	EPILOG_STOP		(10),
	EPILOG			(11),
	SHUTDOWN		(12),
	CANCEL			(13),
	FAILURE			(14),
	CLEANUP			(15),
	UNKNOWN			(16);

	private final int stateId;

	private OneVMState(int stateId) {
		this.stateId = stateId;
	}

	public final int getStateId() {
		return this.stateId;
	}

}
