/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.spi;

import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yangtools.concepts.Identifiable;

public interface InstructionScheduler extends Identifiable<ServiceGroupIdentifier> {
    /**
     * Schedule a new instruction for execution. This method tries to enqueue an instruction. It will return a Future
     * which represents the scheduling progress. When the future becomes successful, the requestor is expected to start
     * executing on the instruction, as specified by the {@link Instruction} contract.
     *
     * @param input Instruction scheduling information
     * @return Scheduling future.
     * @throws SchedulerException if a failure to schedule the instruction occurs.
     */
    ListenableFuture<Instruction> scheduleInstruction(SubmitInstructionInput input) throws SchedulerException;

    /**
     * Returns InstructionID.
     *
     * @return Instruction ID
     */
    @Nonnull
    String getInstructionID();
}
