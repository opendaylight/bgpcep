/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

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
public final class BGPTreatAsWithdrawException extends AbstractBGPException {
    private static final long serialVersionUID = 1L;

    public BGPTreatAsWithdrawException(final @NonNull BGPError error, final @NonNull String format,
            final Object... args) {
        this(error, (Exception) null, format, args);
    }

    public BGPTreatAsWithdrawException(final @NonNull BGPError error, final byte[] data, final @NonNull String format,
            final Object... args) {
        super(String.format(format, args), error, data, null);
    }

    public BGPTreatAsWithdrawException(final @NonNull BGPError error, @Nullable final Exception cause,
            final @NonNull String format, final Object... args) {
        super(String.format(format, args), error, null, cause);
    }
}
