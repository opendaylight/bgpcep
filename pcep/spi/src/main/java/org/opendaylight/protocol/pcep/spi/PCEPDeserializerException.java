/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

/**
 * Used when something occurs during parsing bytes to java objects.
 */
public class PCEPDeserializerException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Used when no exact error (from an RFC or from a draft) is specified.
     *
     * @param err error message describing the error that occurred
     */
    public PCEPDeserializerException(final String err) {
        super(err);
    }

    /**
     * Used when we want to pass also the exception that occurred.
     *
     * @param err error message describing the error that occurred
     * @param cause specific exception that occurred
     */
    public PCEPDeserializerException(final String err, final Throwable cause) {
        super(err, cause);
    }
}
