/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.util.Timeout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CancelFailure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.UncancellableInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.DetailsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InstructionImpl implements Instruction {
    private static final Logger LOG = LoggerFactory.getLogger(InstructionImpl.class);
    private final List<InstructionImpl> dependants = new ArrayList<>();
    private final SettableFuture<Instruction> schedulingFuture;
    private final List<InstructionImpl> dependencies;
    private final QueueInstruction queue;
    private final InstructionId id;
    private SettableFuture<ExecutionResult<Details>> executionFuture;
    private InstructionStatus status = InstructionStatus.Queued;
    private Details heldUpDetails;
    private Timeout timeout;

    InstructionImpl(final QueueInstruction queue, final SettableFuture<Instruction> future, final InstructionId id,
            final List<InstructionImpl> dependencies, final Timeout timeout) {
        this.schedulingFuture = requireNonNull(future);
        this.dependencies = requireNonNull(dependencies);
        this.timeout = requireNonNull(timeout);
        this.queue = requireNonNull(queue);
        this.id = requireNonNull(id);
    }

    InstructionId getId() {
        return this.id;
    }

    synchronized InstructionStatus getStatus() {
        return this.status;
    }

    private synchronized void setStatus(final InstructionStatus status, final Details details) {
        // Set the status
        this.status = status;
        LOG.debug("Instruction {} transitioned to status {}", this.id, status);

        // Send out a notification
        this.queue.instructionUpdated(status, details);

        switch (status) {
            case Cancelled:
            case Failed:
            case Unknown:
                cancelDependants();
                break;
            case Executing:
            case Queued:
            case Scheduled:
            case Successful:
                break;
            default:
                break;
        }
    }

    @GuardedBy("this")
    private void cancelTimeout() {
        if (this.timeout != null) {
            this.timeout.cancel();
            this.timeout = null;
        }
    }

    public synchronized void timeout() {
        if (this.timeout == null) {
            return;
        }
        this.timeout = null;
        switch (this.status) {
            case Cancelled:
            case Failed:
            case Successful:
                LOG.debug("Instruction {} has status {}, timeout is a no-op", this.id, this.status);
                break;
            case Unknown:
                LOG.warn("Instruction {} has status {} before timeout completed", this.id, this.status);
                break;
            case Executing:
                LOG.info("Instruction {} timed out while executing, transitioning into Unknown", this.id);
                setStatus(InstructionStatus.Unknown, null);
                cancelDependants();
                break;
            case Queued:
                LOG.debug("Instruction {} timed out while Queued, cancelling it", this.id);
                cancelInstrunction();
                break;
            case Scheduled:
                LOG.debug("Instruction {} timed out while Scheduled, cancelling it", this.id);
                cancel(this.heldUpDetails);
                break;
            default:
                break;
        }
    }

    private synchronized void cancelInstrunction() {
        final List<InstructionId> ids = new ArrayList<>();
        for (final InstructionImpl instruction : this.dependencies) {
            if (instruction.getStatus() != InstructionStatus.Successful) {
                ids.add(instruction.getId());
            }
        }
        cancel(new DetailsBuilder().setUnmetDependencies(ids).build());
    }

    @GuardedBy("this")
    private void cancelDependants() {
        final Details details = new DetailsBuilder().setUnmetDependencies(ImmutableList.of(this.id)).build();
        for (final InstructionImpl instruction : this.dependants) {
            instruction.tryCancel(details);
        }
    }

    @GuardedBy("this")
    private void cancel(final Details details) {
        cancelTimeout();
        this.schedulingFuture.cancel(false);
        setStatus(InstructionStatus.Cancelled, details);
    }

    synchronized Class<? extends CancelFailure> tryCancel(final Details details) {
        switch (this.status) {
            case Cancelled:
            case Executing:
            case Failed:
            case Successful:
            case Unknown:
                LOG.debug("Instruction {} can no longer be cancelled due to status {}", this.id, this.status);
                return UncancellableInstruction.class;
            case Queued:
            case Scheduled:
                cancel(details);
                return null;
            default:
                throw new IllegalStateException("Unhandled instruction state " + this.status);
        }
    }

    @Override
    public synchronized boolean checkedExecutionStart() {
        if (this.status != InstructionStatus.Scheduled) {
            return false;
        }

        setStatus(InstructionStatus.Executing, null);
        return true;
    }

    @Override
    public synchronized boolean executionHeldUp(final Details details) {
        if (this.status != InstructionStatus.Scheduled) {
            return false;
        }

        this.heldUpDetails = details;
        return true;
    }

    @Override
    public void executionCompleted(final InstructionStatus status, final Details details) {
        final ExecutionResult<Details> result;

        synchronized (this) {
            Preconditions.checkState(this.executionFuture != null);

            cancelTimeout();

            // We reuse the preconditions set down in this class
            result = new ExecutionResult<>(status, details);
            setStatus(status, details);
            this.executionFuture.set(result);
        }
    }

    synchronized void addDependant(final InstructionImpl instruction) {
        this.dependants.add(instruction);
    }

    private synchronized void removeDependant(final InstructionImpl instruction) {
        this.dependants.remove(instruction);
    }

    private synchronized void removeDependency(final InstructionImpl other) {
        this.dependencies.remove(other);
    }

    synchronized Iterator<InstructionImpl> getDependants() {
        return this.dependants.iterator();
    }

    synchronized void clean() {
        for (final InstructionImpl dependency : this.dependencies) {
            dependency.removeDependant(this);
        }
        this.dependencies.clear();

        for (final InstructionImpl dependant : this.dependants) {
            dependant.removeDependency(this);
        }
        this.dependants.clear();

        this.queue.instructionRemoved();
    }

    private Boolean checkDependencies() {
        boolean ready = true;
        final List<InstructionId> unmet = new ArrayList<>();
        for (final InstructionImpl instruction : this.dependencies) {
            switch (instruction.getStatus()) {
                case Cancelled:
                case Failed:
                case Unknown:
                    unmet.add(instruction.getId());
                    break;
                case Executing:
                case Queued:
                case Scheduled:
                    ready = false;
                    break;
                case Successful:
                    // No-op
                    break;
                default:
                    break;
            }
        }
        if (!unmet.isEmpty()) {
            LOG.warn("Instruction {} was Queued, while some dependencies were resolved unsuccessfully, cancelling it",
                    this.id);
            cancel(new DetailsBuilder().setUnmetDependencies(unmet).build());
            return false;
        }
        return ready;
    }

    synchronized ListenableFuture<ExecutionResult<Details>> ready() {
        Preconditions.checkState(this.status == InstructionStatus.Queued);
        Preconditions.checkState(this.executionFuture == null);
        /*
         * Check all vertices we depend on. We start off as ready for
         * scheduling. If we encounter a cancelled/failed/unknown
         * dependency, we cancel this instruction (and cascade). If we
         * encounter an executing/queued/scheduled dependency, we hold
         * of scheduling this one.
         */
        if (!checkDependencies()) {
            return null;
        }
        LOG.debug("Instruction {} is ready for execution", this.id);
        setStatus(InstructionStatus.Scheduled, null);
        this.executionFuture = SettableFuture.create();
        this.schedulingFuture.set(this);
        return this.executionFuture;
    }
}
