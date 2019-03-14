/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;

/**
 * Exception reported when a {@link ParameterSerializer} detects its output does not fit 255 bytes and hence cannot
 * be held in plain RFC4271 OPEN message.
 */
@Beta
public final class ParameterLengthOverflowException extends Exception {
    private static final long serialVersionUID = 1L;

    public ParameterLengthOverflowException(final String message) {
        super(requireNonNull(message));
    }

    public static void throwIf(final boolean expression, final String format, final Object... args)
            throws ParameterLengthOverflowException {
        if (expression) {
            throw new ParameterLengthOverflowException(String.format(format, args));
        }
    }
}
