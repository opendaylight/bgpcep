/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;


public class BmpDeserializationException extends Exception {

    private static final long serialVersionUID = -7486453568941661756L;

    /**
     * Creates new BmpDeserializationException with specific error message.
     * @param message message bound with this exception
     */
    public BmpDeserializationException(final String message) {
        super(message, null);
    }
    /**
     * @param message message bound with this exception
     * @param cause   cause for the error
     */
    public BmpDeserializationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}