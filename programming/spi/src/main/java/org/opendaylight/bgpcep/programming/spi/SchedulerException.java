/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.submit.instruction.output.result.failure._case.Failure;

public class SchedulerException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Failure failure;

    public SchedulerException(final String message, final Failure failure) {
        super(message);
        this.failure = failure;
    }

    public final Failure getFailure() {
        return this.failure;
    }
}
