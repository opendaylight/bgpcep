/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.framework.DocumentedException;

/**
 * There are several errors documented in RFC5440 or in draft, that have
 * specific meaning for the PCE. This exception is used, when any of those
 * errors occurs.
 */
public class PCEPDocumentedException extends DocumentedException {

	private static final long serialVersionUID = 5146586011100522025L;

	private final PCEPErrors error;

	private static final Logger logger = LoggerFactory.getLogger(PCEPDocumentedException.class);

	/**
	 * Used when an error occured that is described in rfc or draft
	 *
	 * @param message
	 * 			message bound with this exception
	 * @param error
	 *            specific documented error
	 */
	public PCEPDocumentedException(String message, PCEPErrors error) {
		super(message);
		this.error = error;
		logger.error("Error = " + error, this);
	}

	/**
	 * Returns specific documented error
	 *
	 * @return documented error
	 */
	public PCEPErrors getError() {
		return this.error;
	}
}
