/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.spi;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.submit.instruction.output.result.FailureCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.submit.instruction.output.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInstructionExecutor implements FutureCallback<Instruction> {

    private static final class InstructionCallback implements FutureCallback<OperationResult> {

        private final Instruction insn;

        public InstructionCallback(final Instruction insn) {
            this.insn = insn;
        }

        @Override
        public void onSuccess(final OperationResult result) {
            if (result.getFailure() != null) {
                switch (result.getFailure()) {
                case Failed:
                case NoAck:
                    this.insn.executionCompleted(InstructionStatus.Failed, null);
                    break;
                case Unsent:
                    this.insn.executionCompleted(InstructionStatus.Cancelled, null);
                    break;
                default:
                    break;
                }
            } else {
                this.insn.executionCompleted(InstructionStatus.Successful, null);
            }
        }

        @Override
        public void onFailure(final Throwable t) {
            this.insn.executionCompleted(InstructionStatus.Failed, null);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInstructionExecutor.class);
    private final SubmitInstructionInput input;

    protected AbstractInstructionExecutor(final SubmitInstructionInput input) {
        this.input = Preconditions.checkNotNull(input);
    }

    public final SubmitInstructionInput getInput() {
        return this.input;
    }

    public static FailureCase schedule(final InstructionScheduler scheduler, final AbstractInstructionExecutor fwd) {
        final ListenableFuture<Instruction> s;
        try {
            s = scheduler.scheduleInstruction(fwd.getInput());
        } catch (final SchedulerException e) {
            LOG.info("Instuction {} failed to schedule", e.getMessage(), e);
            return new FailureCaseBuilder().setFailure(e.getFailure()).build();
        }
        Futures.addCallback(s, fwd, MoreExecutors.directExecutor());
        return null;
    }

    protected abstract ListenableFuture<OperationResult> invokeOperation();

    @Override
    public void onSuccess(final Instruction insn) {
        if (insn.checkedExecutionStart()) {
            final ListenableFuture<OperationResult> s = invokeOperation();
            Futures.addCallback(s, new InstructionCallback(insn), MoreExecutors.directExecutor());
        }
    }

    @Override
    public void onFailure(final Throwable t) {
        LOG.debug("Instruction {} cancelled", this.input, t);
    }
}
