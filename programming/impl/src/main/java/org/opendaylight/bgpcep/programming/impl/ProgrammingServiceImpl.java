/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.bgpcep.programming.NanotimeUtil;
import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CancelInstructionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CancelInstructionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CleanInstructionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CleanInstructionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CleanInstructionsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.DeadOnArrival;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.DuplicateInstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatusChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionsQueue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionsQueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionsQueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.Nanotime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.UnknownInstruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.UnknownPreconditionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.queue.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.queue.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.submit.instruction.output.result.failure._case.FailureBuilder;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProgrammingServiceImpl implements ClusterSingletonService, InstructionScheduler, ProgrammingService {
    private static final Logger LOG = LoggerFactory.getLogger(ProgrammingServiceImpl.class);

    private final Map<InstructionId, InstructionImpl> insns = new HashMap<>();
    private final InstanceIdentifier<InstructionsQueue> qid;
    private final NotificationPublishService notifs;
    private final ListeningExecutorService executor;
    private final DataBroker dataProvider;
    private final Timer timer;
    private final String instructionId;
    private final ServiceGroupIdentifier sgi;
    private final ClusterSingletonServiceRegistration csspReg;
    private final RpcProviderService rpcProviderRegistry;
    @GuardedBy("this")
    private ObjectRegistration<ProgrammingService> reg;

    private final class InstructionPusher implements QueueInstruction {
        private final InstructionBuilder builder = new InstructionBuilder();

        InstructionPusher(final InstructionId id, final Nanotime deadline) {
            builder.setDeadline(deadline);
            builder.setId(id);
            builder.withKey(new InstructionKey(id));
            builder.setStatus(InstructionStatus.Queued);
        }

        @Override
        public void instructionUpdated(final InstructionStatus status, final Details details) {
            if (!status.equals(builder.getStatus())) {
                builder.setStatus(status);

                final WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
                wt.put(LogicalDatastoreType.OPERATIONAL,
                        qid.child(
                                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming
                                        .rev150720.instruction.queue.Instruction.class,
                                new InstructionKey(builder.getId())), builder.build());
                wt.commit().addCallback(new FutureCallback<CommitInfo>() {
                    @Override
                    public void onSuccess(final CommitInfo result) {
                        LOG.debug("Instruction Queue {} updated", qid);
                    }

                    @Override
                    public void onFailure(final Throwable trw) {
                        LOG.error("Failed to update Instruction Queue {}", qid, trw);
                    }
                }, MoreExecutors.directExecutor());
            }

            try {
                notifs.putNotification(new InstructionStatusChangedBuilder()
                        .setId(builder.getId()).setStatus(status).setDetails(details).build());
            } catch (final InterruptedException e) {
                LOG.debug("Failed to publish notification", e);
            }
        }

        @Override
        public void instructionRemoved() {
            final WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
            wt.delete(LogicalDatastoreType.OPERATIONAL, qid.child(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction
                            .queue.Instruction.class,
                    new InstructionKey(builder.getId())));
            wt.commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.debug("Instruction Queue {} removed", qid);
                }

                @Override
                public void onFailure(final Throwable trw) {
                    LOG.error("Failed to remove Instruction Queue {}", qid, trw);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    ProgrammingServiceImpl(final DataBroker dataProvider, final NotificationPublishService notifs,
            final ListeningExecutorService executor, final RpcProviderService rpcProviderRegistry,
            final ClusterSingletonServiceProvider cssp, final Timer timer, final String instructionId) {
        this.dataProvider = requireNonNull(dataProvider);
        this.instructionId = requireNonNull(instructionId);
        this.notifs = requireNonNull(notifs);
        this.executor = requireNonNull(executor);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.timer = requireNonNull(timer);
        qid = KeyedInstanceIdentifier.builder(InstructionsQueue.class,
                new InstructionsQueueKey(this.instructionId)).build();
        sgi = ServiceGroupIdentifier.create(this.instructionId + "-service-group");
        LOG.info("Creating Programming Service {}.", sgi.getName());
        csspReg = cssp.registerClusterSingletonService(this);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.info("Instruction Queue service {} instantiated", sgi.getName());
        reg = rpcProviderRegistry.registerRpcImplementation(ProgrammingService.class, this);

        final WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
        wt.put(LogicalDatastoreType.OPERATIONAL, qid, new InstructionsQueueBuilder()
                .withKey(new InstructionsQueueKey(instructionId)).setInstruction(Map.of()).build());
        wt.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Instruction Queue {} added", qid);
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed to add Instruction Queue {}", qid, trw);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return sgi;
    }

    @Override
    public ListenableFuture<RpcResult<CancelInstructionOutput>> cancelInstruction(final CancelInstructionInput input) {
        return executor.submit(() -> realCancelInstruction(input));
    }

    @Override
    public ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanInstructions(final CleanInstructionsInput input) {
        return executor.submit(() -> realCleanInstructions(input));
    }

    private synchronized RpcResult<CancelInstructionOutput> realCancelInstruction(final CancelInstructionInput input) {
        final InstructionImpl instruction = insns.get(input.getId());
        if (instruction == null) {
            LOG.debug("Instruction {} not present in the graph", input.getId());

            final CancelInstructionOutput out = new CancelInstructionOutputBuilder()
                    .setFailure(UnknownInstruction.class).build();
            return SuccessfulRpcResult.create(out);
        }

        return SuccessfulRpcResult.create(new CancelInstructionOutputBuilder()
                .setFailure(instruction.tryCancel(null)).build());
    }

    private synchronized RpcResult<CleanInstructionsOutput> realCleanInstructions(final CleanInstructionsInput input) {
        final Set<InstructionId> failed = new HashSet<>();

        for (final InstructionId id : input.getId()) {
            // Find the instruction
            final InstructionImpl instruction = insns.get(id);
            if (instruction == null) {
                LOG.debug("Instruction {} not present in the graph", input.getId());
                failed.add(id);
                continue;
            }

            // Check its status
            switch (instruction.getStatus()) {
                case Cancelled:
                case Failed:
                case Successful:
                    break;
                case Executing:
                case Queued:
                case Scheduled:
                case Unknown:
                    LOG.debug("Instruction {} cannot be cleaned because of it's in state {}",
                            id, instruction.getStatus());
                    failed.add(id);
                    continue;
                default:
                    break;
            }

            // The instruction is in a terminal state, we need to just unlink
            // it from its dependencies and dependents
            instruction.clean();

            insns.remove(id);
            LOG.debug("Instruction {} cleaned successfully", id);
        }

        final CleanInstructionsOutputBuilder ob = new CleanInstructionsOutputBuilder();
        ob.setUnflushed(failed);

        return SuccessfulRpcResult.create(ob.build());
    }

    private List<InstructionImpl> checkDependencies(final SubmitInstructionInput input) throws SchedulerException {
        final List<InstructionImpl> dependencies = collectDependencies(input);
        // Check if all dependencies are non-failed
        final Set<InstructionId> unmet = checkIfUnfailed(dependencies);
        /*
         *  Some dependencies have failed, declare the request dead-on-arrival
         *  and fail the operation.
         */
        if (!unmet.isEmpty()) {
            throw new SchedulerException("Instruction's dependencies are already unsuccessful", new FailureBuilder()
                    .setType(DeadOnArrival.class).setFailedPreconditions(unmet).build());
        }
        return dependencies;
    }

    private List<InstructionImpl> collectDependencies(final SubmitInstructionInput input) throws SchedulerException {
        final List<InstructionImpl> dependencies = new ArrayList<>();
        for (final InstructionId pid : input.getPreconditions()) {
            final InstructionImpl instruction = insns.get(pid);
            if (instruction == null) {
                LOG.info("Instruction {} depends on {}, which is not a known instruction", input.getId(), pid);
                throw new SchedulerException("Unknown dependency ID specified",
                        new FailureBuilder().setType(UnknownPreconditionId.class).build());
            }
            dependencies.add(instruction);
        }
        return dependencies;
    }

    private static Set<InstructionId> checkIfUnfailed(final List<InstructionImpl> dependencies) {
        final Set<InstructionId> unmet = new HashSet<>();
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
                default:
                    break;
            }
        }
        return unmet;
    }

    @Override
    public synchronized ListenableFuture<Instruction> scheduleInstruction(final SubmitInstructionInput input) throws
            SchedulerException {
        final InstructionId id = input.getId();
        if (insns.get(id) != null) {
            LOG.info("Instruction ID {} already present", id);
            throw new SchedulerException("Instruction ID currently in use",
                    new FailureBuilder().setType(DuplicateInstructionId.class).build());
        }

        // First things first: check the deadline
        final Nanotime now = NanotimeUtil.currentTime();
        final BigInteger left = input.getDeadline().getValue().toJava().subtract(now.getValue().toJava());

        if (left.compareTo(BigInteger.ZERO) <= 0) {
            LOG.debug("Instruction {} deadline has already passed by {}ns", id, left);
            throw new SchedulerException("Instruction arrived after specified deadline",
                    new FailureBuilder().setType(DeadOnArrival.class).build());
        }

        // Resolve dependencies
        final List<InstructionImpl> dependencies = checkDependencies(input);

        /*
         * All pre-flight checks done are at this point, the following
         * steps can only fail in catastrophic scenarios (OOM and the
         * like).
         */

        // Schedule a timeout for the instruction
        final Timeout t = timer.newTimeout(timeout -> timeoutInstruction(input.getId()), left.longValue(),
                TimeUnit.NANOSECONDS);

        // Put it into the instruction list
        final SettableFuture<Instruction> ret = SettableFuture.create();
        final InstructionImpl instruction = new InstructionImpl(new InstructionPusher(id, input.getDeadline()), ret, id,
                dependencies, t);
        insns.put(id, instruction);

        // Attach it into its dependencies
        for (final InstructionImpl d : dependencies) {
            d.addDependant(instruction);
        }

        /*
         * All done. The next part is checking whether the instruction can
         * run, which we can figure out after sending out the acknowledgement.
         * This task should be ingress-weighed, so we reinsert it into the
         * same execution service.
         */
        executor.submit(() -> tryScheduleInstruction(instruction));

        return ret;
    }

    @Override
    public String getInstructionID() {
        return instructionId;
    }

    private synchronized void timeoutInstruction(final InstructionId id) {
        final InstructionImpl instruction = insns.get(id);
        if (instruction == null) {
            LOG.warn("Instruction {} timed out, but not found in the queue", id);
            return;
        }

        instruction.timeout();
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private synchronized void tryScheduleDependants(final InstructionImpl instruction) {
        // Walk all dependants and try to schedule them
        final Iterator<InstructionImpl> it = instruction.getDependants();
        while (it.hasNext()) {
            tryScheduleInstruction(it.next());
        }
    }

    private synchronized void tryScheduleInstruction(final InstructionImpl instruction) {
        final ListenableFuture<ExecutionResult<Details>> f = instruction.ready();
        if (f != null) {
            Futures.addCallback(f, new FutureCallback<ExecutionResult<Details>>() {
                @Override
                public void onSuccess(final ExecutionResult<Details> result) {
                    tryScheduleDependants(instruction);
                }

                @Override
                public void onFailure(final Throwable trw) {
                    LOG.error("Instruction {} failed to execute", instruction.getId(), trw);
                }
            }, MoreExecutors.directExecutor());
        }

    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        LOG.info("Closing Instruction Queue service {}", sgi.getName());

        if (reg != null) {
            reg.close();
            reg = null;
        }
        for (final InstructionImpl instruction : insns.values()) {
            instruction.tryCancel(null);
        }
        // Workaround for BUG-2283
        final WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
        wt.delete(LogicalDatastoreType.OPERATIONAL, qid);

        final FluentFuture<? extends CommitInfo> future = wt.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Instruction Queue {} removed", qid);
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed to shutdown Instruction Queue {}", qid, trw);
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    @Override
    public synchronized void close() {
        csspReg.close();
    }
}
