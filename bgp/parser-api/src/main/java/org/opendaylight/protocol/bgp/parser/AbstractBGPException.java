/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Abstract class supporting common aspects of {@link BGPDocumentedException} and {@link BGPTreatAsWithdrawException}.
 */
public abstract class AbstractBGPException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final byte @NonNull [] EMPTY = new byte[0];

    private final @NonNull BGPError error;
    private final byte[] data;

    /**
     * Used when an error occurred that is described in an RFC or a draft.
     *
     * @param message message bound with this exception
     * @param error specific documented error
     * @param data data associated with the error
     * @param cause cause for the error
     */
    AbstractBGPException(final String message, final BGPError error, final byte[] data, final Exception cause) {
        super(message, cause);
        this.error = requireNonNull(error);
        this.data = data == null || data.length == 0 ? null : data.clone();
    }

    /**
     * Returns specific documented error.
     *
     * @return documented error
     */
    public final BGPError getError() {
        return error;
    }

    /**
     * Returns data associated with this error.
     *
     * @return byte array data
     */
    public final byte @NonNull [] getData() {
        return data != null ? data.clone() : EMPTY;
    }
}
