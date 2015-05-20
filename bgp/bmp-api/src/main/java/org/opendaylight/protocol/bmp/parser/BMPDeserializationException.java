/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.parser;
/**
 * Created by cgasparini on 18.5.2015.
 */

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There are several errors documented in RFC*** or in draft, that have specific meaning for the BMP. This exception is
 * used, when any of those errors occurs.
 */
public final class BMPDeserializationException extends Exception {

    private static final Logger LOG = LoggerFactory.getLogger(BMPDeserializationException.class);

    private static final long serialVersionUID = -7486453568941661756L;

    private final byte[] data;

    /**
     * @param message message bound with this exception
     */
    public BMPDeserializationException(final String message) {
        this(message, null, null);
    }
    /**
     * @param message message bound with this exception
     * @param cause   cause for the error
     */
    public BMPDeserializationException(final String message, final Exception cause) {
        this(message, null, cause);
    }


    /**
     * @param message message bound with this exception
     * @param data    data associated with the error
     */
    public BMPDeserializationException(final String message, final byte[] data) {
        this(message, data, null);
    }

    /**
     *
     * @param message message bound with this exception
     * @param data    data associated with the error
     * @param cause   cause for the error
     */
    public BMPDeserializationException(final String message, final byte[] data, final Exception cause) {
        super(message, cause);
        this.data = data == null ? null : Arrays.copyOf(data, data.length);
        LOG.error("Error = {}", this);
    }

    /**
     * Returns data associated with this error.
     *
     * @return byte array data
     */
    public byte[] getData() {
        return (this.data != null) ? Arrays.copyOf(this.data, this.data.length) : new byte[0];
    }
}
