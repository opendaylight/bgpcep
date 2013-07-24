/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.protocol.framework.TerminationReason;

/**
 * Used as a reason when a documented error was the cause of the
 * termination of a session.
 */
public final class PCEPErrorTermination implements TerminationReason {

	private final PCEPErrors error;

	/**
	 * Creates new Termination.
	 * @param error Error that happened.
	 */
	public PCEPErrorTermination(PCEPErrors error) {
		this.error = error;
	}

	/* (non-Javadoc)
	 * @see org.opendaylight.protocol.pcep.PCEPTerminationReason#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return this.error.toString();
	}

}
