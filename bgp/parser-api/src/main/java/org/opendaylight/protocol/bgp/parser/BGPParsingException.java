/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.framework.DeserializerException;

/**
 *
 * Used when something occurs during the parsing to get Update Message.
 *
 */
public class BGPParsingException extends DeserializerException {

	private static final long serialVersionUID = -6893285837086620580L;

	private final String message;

	/**
	 *
	 * @param err error message string.
	 */
	public BGPParsingException(final String err) {
		super(err);
		this.message = err;
	}

	/**
	 *
	 * @param message exception message
	 * @param cause primary exception
	 */
	public BGPParsingException(final String message, final Exception cause){
		super(message, cause);
		this.message = message;
	}

	/**
	 *
	 * @return error message.
	 */
	public String getError() {
		return this.message;
	}
}
