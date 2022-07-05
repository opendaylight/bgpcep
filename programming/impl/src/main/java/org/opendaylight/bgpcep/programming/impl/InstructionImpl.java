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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.util.Timeout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.lock.qual.Holding;
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
        schedulingFuture = requireNonNull(future);
        this.dependencies = requireNonNull(dependencies);
        this.timeout = requireNonNull(timeout);
        this.queue = requireNonNull(queue);
        this.id = requireNonNull(id);
    }

    InstructionId getId() {
        return id;
    }

    synchronized InstructionStatus getStatus() {
        return status;
    }

    private synchronized void setStatus(final InstructionStatus newStatus, final Details details) {
        // Set the status
        status = newStatus;
        LOG.debug("Instruction {} transitioned to status {}", id, newStatus);

        // Send out a notification
        queue.instructionUpdated(newStatus, details);

        switch (newStatus) {
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

    @Holding("this")
    private void cancelTimeout() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    public synchronized void timeout() {
        if (timeout == null) {
            return;
        }
        timeout = null;
        switch (status) {
            case Cancelled:
            case Failed:
            case Successful:
                LOG.debug("Instruction {} has status {}, timeout is a no-op", id, status);
                break;
            case Unknown:
                LOG.warn("Instruction {} has status {} before timeout completed", id, status);
                break;
            case Executing:
                LOG.info("Instruction {} timed out while executing, transitioning into Unknown", id);
                setStatus(InstructionStatus.Unknown, null);
                cancelDependants();
                break;
            case Queued:
                LOG.debug("Instruction {} timed out while Queued, cancelling it", id);
                cancelInstrunction();
                break;
            case Scheduled:
                LOG.debug("Instruction {} timed out while Scheduled, cancelling it", id);
                cancel(heldUpDetails);
                break;
            default:
                break;
        }
    }

    private synchronized void cancelInstrunction() {
        final Set<InstructionId> ids = new HashSet<>();
        for (final InstructionImpl instruction : dependencies) {
            if (instruction.getStatus() != InstructionStatus.Successful) {
                ids.add(instruction.getId());
            }
        }
        cancel(new DetailsBuilder().setUnmetDependencies(ids).build());
    }

    @Holding("this")
    private void cancelDependants() {
        final Details details = new DetailsBuilder().setUnmetDependencies(Set.of(id)).build();
        for (final InstructionImpl instruction : dependants) {
            instruction.tryCancel(details);
        }
    }

    @Holding("this")
    private void cancel(final Details details) {
        cancelTimeout();
        schedulingFuture.cancel(false);
        setStatus(InstructionStatus.Cancelled, details);
    }

    synchronized CancelFailure tryCancel(final Details details) {
        switch (status) {
            case Cancelled:
            case Executing:
            case Failed:
            case Successful:
            case Unknown:
                LOG.debug("Instruction {} can no longer be cancelled due to status {}", id, status);
                return UncancellableInstruction.VALUE;
            case Queued:
            case Scheduled:
                cancel(details);
                return null;
            default:
                throw new IllegalStateException("Unhandled instruction state " + status);
        }
    }

    @Override
    public synchronized boolean checkedExecutionStart() {
        if (status != InstructionStatus.Scheduled) {
            return false;
        }

        setStatus(InstructionStatus.Executing, null);
        return true;
    }

    @Override
    public synchronized boolean executionHeldUp(final Details details) {
        if (status != InstructionStatus.Scheduled) {
            return false;
        }

        heldUpDetails = details;
        return true;
    }

    @Override
    public void executionCompleted(final InstructionStatus newStatus, final Details details) {
        final ExecutionResult<Details> result;

        synchronized (this) {
            Preconditions.checkState(executionFuture != null);

            cancelTimeout();

            // We reuse the preconditions set down in this class
            result = new ExecutionResult<>(newStatus, details);
            setStatus(newStatus, details);
            executionFuture.set(result);
        }
    }

    synchronized void addDependant(final InstructionImpl instruction) {
        dependants.add(instruction);
    }

    private synchronized void removeDependant(final InstructionImpl instruction) {
        dependants.remove(instruction);
    }

    private synchronized void removeDependency(final InstructionImpl other) {
        dependencies.remove(other);
    }

    synchronized Iterator<InstructionImpl> getDependants() {
        return dependants.iterator();
    }

    synchronized void clean() {
        for (final InstructionImpl dependency : dependencies) {
            dependency.removeDependant(this);
        }
        dependencies.clear();

        for (final InstructionImpl dependant : dependants) {
            dependant.removeDependency(this);
        }
        dependants.clear();

        queue.instructionRemoved();
    }

    private Boolean checkDependencies() {
        boolean ready = true;
        final Set<InstructionId> unmet = new HashSet<>();
        for (final InstructionImpl instruction : dependencies) {
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
                    id);
            cancel(new DetailsBuilder().setUnmetDependencies(unmet).build());
            return false;
        }
        return ready;
    }

    synchronized ListenableFuture<ExecutionResult<Details>> ready() {
        Preconditions.checkState(status == InstructionStatus.Queued);
        Preconditions.checkState(executionFuture == null);
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
        LOG.debug("Instruction {} is ready for execution", id);
        setStatus(InstructionStatus.Scheduled, null);
        executionFuture = SettableFuture.create();
        schedulingFuture.set(this);
        return executionFuture;
    }
}
