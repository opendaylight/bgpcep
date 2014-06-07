/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.opendaylight.bgpcep.programming.NanotimeUtil;
import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DeadOnArrival;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.DuplicateInstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionQueue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionQueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatusChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.Nanotime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.UnknownPreconditionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.InstructionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure._case.FailureBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProgrammingServiceImpl implements AutoCloseable, InstructionScheduler, ProgrammingService {
    private static final Logger LOG = LoggerFactory.getLogger(ProgrammingServiceImpl.class);

    private final Map<InstructionId, InstructionImpl> insns = new HashMap<>();
    private final InstanceIdentifier<InstructionQueue> qid;
    private final NotificationProviderService notifs;
    private final ListeningExecutorService executor;
    private final DataProviderService dataProvider;
    private final Timer timer;

    private final class InstructionPusher implements QueueInstruction {
        private final InstructionsBuilder builder = new InstructionsBuilder();

        InstructionPusher(final InstructionId id, final Nanotime deadline) {
            builder.setDeadline(deadline);
            builder.setId(id);
            builder.setKey(new InstructionsKey(id));
            builder.setStatus(InstructionStatus.Queued);
        }

        @Override
        public void instructionUpdated(final InstructionStatus status, final Details details) {
            if (!status.equals(builder.getStatus())) {
                builder.setStatus(status);

                final DataModificationTransaction t = dataProvider.beginTransaction();
                t.putOperationalData(
                        qid.child(
                                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.Instruction.class,
                                new InstructionKey(builder.getId())), builder.build());
                t.commit();
            }

            notifs.publish(new InstructionStatusChangedBuilder().setId(builder.getId()).setStatus(status).setDetails(details).build());
        }

        @Override
        public void instructionRemoved() {
            final DataModificationTransaction t = dataProvider.beginTransaction();
            t.removeOperationalData(qid.child(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.Instruction.class,
                    new InstructionKey(builder.getId())));
            t.commit();
        }
    }

    public ProgrammingServiceImpl(final DataProviderService dataProvider, final NotificationProviderService notifs,
            final ListeningExecutorService executor, final Timer timer) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
        this.notifs = Preconditions.checkNotNull(notifs);
        this.executor = Preconditions.checkNotNull(executor);
        this.timer = Preconditions.checkNotNull(timer);
        qid = InstanceIdentifier.builder(InstructionQueue.class).toInstance();

        final DataModificationTransaction t = dataProvider.beginTransaction();
        Preconditions.checkState(t.readOperationalData(qid) == null, "Conflicting instruction queue found");

        t.putOperationalData(
                qid,
                new InstructionQueueBuilder().setInstruction(
                        Collections.<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.Instruction> emptyList()).build());
        t.commit();
    }

    @Override
    public ListenableFuture<RpcResult<CancelInstructionOutput>> cancelInstruction(final CancelInstructionInput input) {
        return this.executor.submit(new Callable<RpcResult<CancelInstructionOutput>>() {
            @Override
            public RpcResult<CancelInstructionOutput> call() {
                return realCancelInstruction(input);
            }
        });
    }

    @Override
    public ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanInstructions(final CleanInstructionsInput input) {
        return this.executor.submit(new Callable<RpcResult<CleanInstructionsOutput>>() {
            @Override
            public RpcResult<CleanInstructionsOutput> call() {
                return realCleanInstructions(input);
            }
        });
    }

    private synchronized RpcResult<CancelInstructionOutput> realCancelInstruction(final CancelInstructionInput input) {
        final InstructionImpl i = this.insns.get(input.getId());
        if (i == null) {
            LOG.debug("Instruction {} not present in the graph", input.getId());

            final CancelInstructionOutput out = new CancelInstructionOutputBuilder().setFailure(UnknownInstruction.class).build();
            return SuccessfulRpcResult.create(out);
        }

        return SuccessfulRpcResult.create(new CancelInstructionOutputBuilder().setFailure(i.tryCancel(null)).build());
    }

    private synchronized RpcResult<CleanInstructionsOutput> realCleanInstructions(final CleanInstructionsInput input) {
        final List<InstructionId> failed = new ArrayList<>();

        for (final InstructionId id : input.getId()) {
            // Find the instruction
            final InstructionImpl i = this.insns.get(id);
            if (i == null) {
                LOG.debug("Instruction {} not present in the graph", input.getId());
                failed.add(id);
                continue;
            }

            // Check its status
            switch (i.getStatus()) {
            case Cancelled:
            case Failed:
            case Successful:
                break;
            case Executing:
            case Queued:
            case Scheduled:
            case Unknown:
                LOG.debug("Instruction {} cannot be cleaned because of it's in state {}", id, i.getStatus());
                failed.add(id);
                continue;
            }

            // The instruction is in a terminal state, we need to just unlink
            // it from its dependencies and dependants
            i.clean();

            insns.remove(i);
            LOG.debug("Instruction {} cleaned successfully", id);
        }

        final CleanInstructionsOutputBuilder ob = new CleanInstructionsOutputBuilder();
        if (!failed.isEmpty()) {
            ob.setUnflushed(failed);
        }

        return SuccessfulRpcResult.create(ob.build());
    }

    @Override
    public synchronized ListenableFuture<Instruction> scheduleInstruction(final SubmitInstructionInput input) throws SchedulerException {
        final InstructionId id = input.getId();
        if (this.insns.get(id) != null) {
            LOG.info("Instruction ID {} already present", id);
            throw new SchedulerException("Instruction ID currently in use", new FailureBuilder().setType(DuplicateInstructionId.class).build());
        }

        // First things first: check the deadline
        final Nanotime now = NanotimeUtil.currentTime();
        final BigInteger left = input.getDeadline().getValue().subtract(now.getValue());

        if (left.compareTo(BigInteger.ZERO) <= 0) {
            LOG.debug("Instruction {} deadline has already passed by {}ns", id, left);
            throw new SchedulerException("Instruction arrived after specified deadline", new FailureBuilder().setType(DeadOnArrival.class).build());
        }

        // Resolve dependencies
        final List<InstructionImpl> dependencies = new ArrayList<>();
        for (final InstructionId pid : input.getPreconditions()) {
            final InstructionImpl i = this.insns.get(pid);
            if (i == null) {
                LOG.info("Instruction {} depends on {}, which is not a known instruction", id, pid);
                throw new SchedulerException("Unknown dependency ID specified", new FailureBuilder().setType(UnknownPreconditionId.class).build());
            }

            dependencies.add(i);
        }

        // Check if all dependencies are non-failed
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
            case Successful:
                break;
            }
        }

        /*
         *  Some dependencies have failed, declare the request dead-on-arrival
         *  and fail the operation.
         */
        if (!unmet.isEmpty()) {
            throw new SchedulerException("Instruction's dependencies are already unsuccessful", new FailureBuilder().setType(
                    DeadOnArrival.class).setFailedPreconditions(unmet).build());
        }

        /*
         * All pre-flight checks done are at this point, the following
         * steps can only fail in catastrophic scenarios (OOM and the
         * like).
         */

        // Schedule a timeout for the instruction
        final Timeout t = this.timer.newTimeout(new TimerTask() {
            @Override
            public void run(final Timeout timeout) {
                timeoutInstruction(input.getId());
            }
        }, left.longValue(), TimeUnit.NANOSECONDS);

        // Put it into the instruction list
        final SettableFuture<Instruction> ret = SettableFuture.create();
        final InstructionImpl i = new InstructionImpl(new InstructionPusher(id, input.getDeadline()), ret, id, dependencies, t);
        this.insns.put(id, i);

        // Attach it into its dependencies
        for (final InstructionImpl d : dependencies) {
            d.addDependant(i);
        }

        /*
         * All done. The next part is checking whether the instruction can
         * run, which we can figure out after sending out the acknowledgement.
         * This task should be ingress-weighed, so we reinsert it into the
         * same execution service.
         */
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                tryScheduleInstruction(i);
            }
        });

        return ret;
    }

    private synchronized void timeoutInstruction(final InstructionId id) {
        final InstructionImpl i = this.insns.get(id);
        if (i == null) {
            LOG.warn("Instruction {} timed out, but not found in the queue", id);
            return;
        }

        i.timeout();
    }

    private synchronized void tryScheduleDependants(final InstructionImpl i) {
        // Walk all dependants and try to schedule them
        final Iterator<InstructionImpl> it = i.getDependants();
        while (it.hasNext()) {
            tryScheduleInstruction(it.next());
        }
    }

    private synchronized void tryScheduleInstruction(final InstructionImpl i) {
        final ListenableFuture<ExecutionResult<Details>> f = i.ready();
        if (f != null) {
            Futures.addCallback(f, new FutureCallback<ExecutionResult<Details>>() {
                @Override
                public void onSuccess(final ExecutionResult<Details> result) {
                    tryScheduleDependants(i);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Instruction {} failed to execute", i.getId(), t);
                }
            });
        }

    }

    @Override
    public synchronized void close() {
        try {
            for (InstructionImpl i : insns.values()) {
                i.tryCancel(null);
            }
        } finally {
            final DataModificationTransaction t = dataProvider.beginTransaction();
            t.removeOperationalData(qid);
            t.commit();
        }
    }
}
