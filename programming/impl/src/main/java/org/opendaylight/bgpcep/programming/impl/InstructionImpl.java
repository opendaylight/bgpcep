/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelFailure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UncancellableInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.DetailsBuilder;
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
        this.schedulingFuture = Preconditions.checkNotNull(future);
        this.dependencies = Preconditions.checkNotNull(dependencies);
        this.timeout = Preconditions.checkNotNull(timeout);
        this.queue = Preconditions.checkNotNull(queue);
        this.id = Preconditions.checkNotNull(id);
    }

    InstructionId getId() {
        return id;
    }

    InstructionStatus getStatus() {
        return status;
    }

    synchronized void setStatus(final InstructionStatus status, final Details details) {
        // Set the status
        this.status = status;
        LOG.debug("Instruction {} transitioned to status {}", id, status);

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
        }
    }

    @GuardedBy("this")
    private void cancelTimeout() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    public synchronized void timeout() {
        if (timeout != null) {
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

                final List<InstructionId> ids = new ArrayList<>();
                for (final InstructionImpl d : dependencies) {
                    if (d.getStatus() != InstructionStatus.Successful) {
                        ids.add(d.getId());
                    }
                }

                cancel(new DetailsBuilder().setUnmetDependencies(ids).build());
                break;
            case Scheduled:
                LOG.debug("Instruction {} timed out while Scheduled, cancelling it", id);
                cancel(heldUpDetails);
                break;
            }
        }
    }

    @GuardedBy("this")
    private void cancelDependants() {
        final Details details = new DetailsBuilder().setUnmetDependencies(ImmutableList.of(id)).build();
        for (final InstructionImpl d : dependants) {
            d.tryCancel(details);
        }
    }

    @GuardedBy("this")
    private void cancel(final Details details) {
        cancelTimeout();
        schedulingFuture.cancel(false);
        setStatus(InstructionStatus.Cancelled, details);
    }

    synchronized Class<? extends CancelFailure> tryCancel(final Details details) {
        switch (status) {
        case Cancelled:
        case Executing:
        case Failed:
        case Successful:
        case Unknown:
            LOG.debug("Instruction {} can no longer be cancelled due to status {}", id, status);
            return UncancellableInstruction.class;
        case Queued:
        case Scheduled:
            cancel(details);
            return null;
        }

        throw new IllegalStateException("Unhandled instruction state " + status);
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

        this.heldUpDetails = details;
        return true;
    }

    @Override
    public synchronized void executionCompleted(final InstructionStatus status, final Details details) {
        Preconditions.checkState(executionFuture != null);

        cancelTimeout();

        // We reuse the preconditions set down in this class
        final ExecutionResult<Details> result = new ExecutionResult<Details>(status, details);
        setStatus(status, details);
        executionFuture.set(result);
    }

    synchronized void addDependant(final InstructionImpl d) {
        dependants.add(d);
    }

    private synchronized void removeDependant(final InstructionImpl d) {
        dependants.remove(d);
    }

    private synchronized void removeDependency(final InstructionImpl other) {
        dependencies.remove(other);
    }

    synchronized Iterator<InstructionImpl> getDependants() {
        return dependants.iterator();
    }

    synchronized void clean() {
        for (final Iterator<InstructionImpl> it = dependencies.iterator(); it.hasNext();) {
            it.next().removeDependant(this);
        }
        dependencies.clear();

        for (final Iterator<InstructionImpl> it = dependants.iterator(); it.hasNext();) {
            it.next().removeDependency(this);
        }
        dependants.clear();

        this.queue.instructionRemoved();
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
        boolean ready = true;

        final List<InstructionId> unmet = new ArrayList<>();
        for (final InstructionImpl d : dependencies) {
            switch (d.getStatus()) {
            case Cancelled:
            case Failed:
            case Unknown:
                unmet.add(d.getId());
                break;
            case Executing:
            case Queued:
            case Scheduled:
                ready = false;
                break;
            case Successful:
                // No-op
                break;
            }
        }

        if (!unmet.isEmpty()) {
            LOG.warn("Instruction {} was Queued, while some dependencies were resolved unsuccessfully, cancelling it", id);
            cancel(new DetailsBuilder().setUnmetDependencies(unmet).build());
            return null;
        }

        if (!ready) {
            return null;
        }

        LOG.debug("Instruction {} is ready for execution", id);
        setStatus(InstructionStatus.Scheduled, null);
        executionFuture = SettableFuture.create();
        schedulingFuture.set(this);
        return executionFuture;
    }
}