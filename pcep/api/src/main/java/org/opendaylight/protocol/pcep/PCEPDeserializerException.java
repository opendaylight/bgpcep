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

import org.opendaylight.protocol.framework.DeserializerException;

/**
 * Used when something occurs during parsing bytes to java objects.
 */
public class PCEPDeserializerException extends DeserializerException {

	private static final long serialVersionUID = -7511681435692278498L;

	private static final Logger logger = LoggerFactory.getLogger(PCEPDeserializerException.class);

	private final String message;

	/**
	 * Used when no exact error (from rfc or from draft) is specified.
	 *
	 * @param err
	 *            error message describing the error that occurred
	 */
	public PCEPDeserializerException(String err) {
		this(null, err);
	}

	/**
	 * Used when we want to pass also the exception that occurred.
	 *
	 * @param e
	 *            specific exception that occurred
	 * @param err
	 *            error message describing the error that occurred
	 */
	public PCEPDeserializerException(Exception e, String err) {
		super(err, e);
		this.message = err;
		logger.error("", this);
	}

	/**
	 * Returns error message.
	 *
	 * @return error message
	 */
	public String getErrorMessage() {
		return this.message;
	}
}
