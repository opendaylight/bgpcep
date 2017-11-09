/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.Details;

public interface Instruction {
    /**
     * Instruction executors are required to call this method prior to starting executing on the instruction.
     * Implementations of this method are required to transition into Executing state and return true, or into Cancelled
     * state and return false.
     *
     * @return Indication whether the instruction execution should proceed.
     */
    boolean checkedExecutionStart();

    /**
     * Instruction executors can inform about execution hold ups which prevent an otherwise-ready instruction from
     * executing by calling this method. It is recommended they check the return of this method to detect if a
     * cancellation occurred asynchronously.
     *
     * @param details Details which execution is held up
     * @return Indication whether the instruction execution should proceed. If this method returns false,
     *     all subsequent calls to this method as well as {@link #checkedExecutionStart()} will return false.
     */
    boolean executionHeldUp(Details details);

    /**
     * Instruction executors are required to call this method when execution has finished to provide the execution
     * result to the end.
     *
     * @param status  Execution result
     * @param details Execution result details
     */
    void executionCompleted(InstructionStatus status, Details details);
}
