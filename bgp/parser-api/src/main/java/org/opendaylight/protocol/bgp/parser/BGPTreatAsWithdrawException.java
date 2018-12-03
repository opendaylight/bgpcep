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
import org.eclipse.jdt.annotation.Nullable;

/**
 * An exception thrown when the parsing of an attribute results in treat-as-withdraw being applied to the UPDATE
 * message, as per the rules in RFC7606 and related documents.
 *
 * <p>
 * This exception must not be thrown when Revised Error Handling procedures are not in effect.
 *
 * @author Robert Varga
 */
public final class BGPTreatAsWithdrawException extends Exception {
    private static final long serialVersionUID = 1L;

    private final @NonNull BGPError error;

    public BGPTreatAsWithdrawException(final @NonNull BGPError error, final @NonNull String format,
            final Object... args) {
        this(error, null, format, args);
    }

    public BGPTreatAsWithdrawException(final @NonNull BGPError error, @Nullable final Exception cause,
            final @NonNull String format, final Object... args) {
        super(String.format(format, args), cause);
        this.error = requireNonNull(error);
    }

    public @NonNull BGPDocumentedException toDocumentedException() {
        return new BGPDocumentedException(getMessage(), error, this);
    }
}
