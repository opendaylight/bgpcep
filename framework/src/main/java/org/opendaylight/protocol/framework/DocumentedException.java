/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * Documented exception occurrs when an error is thrown that is documented
 * in any RFC or draft for the specific protocol.
 */
public class DocumentedException extends Exception  {

	private static final long serialVersionUID = -3727963789710833704L;

	/**
	 * Creates a documented exception
	 * @param message string
	 */
	public DocumentedException(final String message) {
		this(message, null);
	}

	/**
	 * Creates a documented exception
	 * @param err string
	 * @param e underlying exception
	 */
	public DocumentedException(final String err, final Exception e) {
		super(err, e);
	}
}
