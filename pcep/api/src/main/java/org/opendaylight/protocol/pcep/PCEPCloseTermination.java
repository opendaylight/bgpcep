/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.protocol.framework.TerminationReason;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject.Reason;

/**
 * Used as a reason when one of the regular reasons was the cause of the
 * termination of a session.
 */
public final class PCEPCloseTermination implements TerminationReason {

	private final Reason reason;

	/**
	 * Creates new Termination.
	 * @param reason reason for termination
	 */
	public PCEPCloseTermination(Reason reason) {
		this.reason = reason;
	}

	/* (non-Javadoc)
	 * @see org.opendaylight.protocol.pcep.PCEPTerminationReason#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return this.reason.toString();
	}

}
