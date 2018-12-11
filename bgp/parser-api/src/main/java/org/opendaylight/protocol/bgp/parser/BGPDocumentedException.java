/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import org.opendaylight.protocol.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There are several errors documented in RFC4271 or in draft, that have specific meaning for the BGP.
 * This exception is used, when any of those errors occurs.
 */
public final class BGPDocumentedException extends AbstractBGPException {
    private static final long serialVersionUID = -6212702584439430736L;
    private static final Logger LOG = LoggerFactory.getLogger(BGPDocumentedException.class);

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param error specific documented error
     */
    public BGPDocumentedException(final BGPError error) {
        this(null, error, null, null);
    }

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error specific documented error
     */
    public BGPDocumentedException(final String message, final BGPError error) {
        this(message, error, null, null);
    }

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error specific documented error
     * @param cause cause for the error
     */
    public BGPDocumentedException(final String message, final BGPError error, final Exception cause) {
        this(message, error, null, cause);
    }

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error specific documented error
     * @param data data associated with the error
     */
    public BGPDocumentedException(final String message, final BGPError error, final byte[] data) {
        this(message, error, data, null);
    }

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error specific documented error
     * @param data data associated with the error
     * @param cause cause for the error
     */
    public BGPDocumentedException(final String message, final BGPError error, final byte[] data,
            final Exception cause) {
        super(message, error, data, cause);
        // FIXME: remove this error?
        LOG.error("Error = {}", error, this);
    }

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param cause cause for the error
     */
    public BGPDocumentedException(final BGPTreatAsWithdrawException cause) {
        this(cause.getMessage(), cause.getError(), cause.getData(), cause);
    }

    public static BGPDocumentedException badMessageLength(final String message, final int length) {
        Preconditions.checkArgument(length >= 0 && length <= Values.UNSIGNED_SHORT_MAX_VALUE);

        return new BGPDocumentedException(message, BGPError.BAD_MSG_LENGTH, new byte[] {
            UnsignedBytes.checkedCast(length / (Values.UNSIGNED_BYTE_MAX_VALUE + 1)),
            UnsignedBytes.checkedCast(length % (Values.UNSIGNED_BYTE_MAX_VALUE + 1)) });
    }
}
