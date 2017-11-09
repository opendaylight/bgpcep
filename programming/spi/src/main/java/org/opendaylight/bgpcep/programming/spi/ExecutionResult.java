/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.spi;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.Details;

public final class ExecutionResult<T extends Details> {
    private final InstructionStatus status;
    private final T details;

    public ExecutionResult(final InstructionStatus status, final T details) {
        Preconditions.checkArgument(status == InstructionStatus.Cancelled
                || status == InstructionStatus.Failed
                || status == InstructionStatus.Successful, "Illegal instruction status " + status);
        Preconditions.checkArgument(status != InstructionStatus.Failed || details != null,
                "Failed status requires details");

        this.status = status;
        this.details = details;
    }

    public InstructionStatus getStatus() {
        return this.status;
    }

    public T getDetails() {
        return this.details;
    }

    @Override
    public String toString() {
        return "ExecutionResult [status=" + this.status + ", details=" + this.details + "]";
    }
}
