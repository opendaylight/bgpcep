package org.opendaylight.protocol.bmp.parser;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Created by cgasparini on 18.5.2015.
 */
/**
 *
 * Used when something occurs during the parsing to get the Messages.
 *
 */
public final class BMPParsingException extends Exception {

    private static final long serialVersionUID = -7362677017332129351L;

    /**
     * Creates new BMPParsingException with specific error message.
     *
     * @param err error message string.
     */
    public BMPParsingException(final String err) {
        super(err);
    }

    /**
     * Creates new BGPParsingException with specific message and cause.
     *
     * @param message exception message
     * @param cause primary exception
     */
    public BMPParsingException(final String message, final Exception cause) {
        super(message, cause);
    }
}
