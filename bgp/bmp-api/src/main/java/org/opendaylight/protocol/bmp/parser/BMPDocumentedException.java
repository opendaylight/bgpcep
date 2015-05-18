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

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import java.util.Arrays;

import org.opendaylight.protocol.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There are several errors documented in RFC*** or in draft, that have specific meaning for the BMP. This exception is
 * used, when any of those errors occurs.
 */
public final class BMPDocumentedException extends Exception {

    private static final Logger LOG = LoggerFactory.getLogger(BMPDocumentedException.class);

    private static final long serialVersionUID = -7486453568941661756L;

    private final BMPError error;

    private final byte[] data;

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error   specific documented error
     */
    public BMPDocumentedException(final String message, final BMPError error) {
        this(message, error, null, null);
    }

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error   specific documented error
     * @param cause   cause for the error
     */
    public BMPDocumentedException(final String message, final BMPError error, final Exception cause) {
        this(message, error, null, cause);
    }


    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error   specific documented error
     * @param data    data associated with the error
     */
    public BMPDocumentedException(final String message, final BMPError error, final byte[] data) {
        this(message, error, data, null);
    }

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error   specific documented error
     * @param data    data associated with the error
     * @param cause   cause for the error
     */
    public BMPDocumentedException(final String message, final BMPError error, final byte[] data, final Exception cause) {
        super(message, cause);
        this.error = error;
        this.data = data == null ? null : Arrays.copyOf(data, data.length);
        LOG.error("Error = {}", error, this);
    }

    public static BMPDocumentedException badMessageLength(final String message, final int length) {
        Preconditions.checkArgument(length >= 0 && length <= Values.UNSIGNED_SHORT_MAX_VALUE);

        return new BMPDocumentedException(message, BMPError.BAD_MSG_LENGTH, new byte[]{
            UnsignedBytes.checkedCast(length / (Values.UNSIGNED_BYTE_MAX_VALUE + 1)),
            UnsignedBytes.checkedCast(length % (Values.UNSIGNED_BYTE_MAX_VALUE + 1))});
    }

    /**
     * Returns specific documented error.
     *
     * @return documented error
     */
    public BMPError getError() {
        return this.error;
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
