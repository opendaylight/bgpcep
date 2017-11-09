/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.bgpcep.programming.NanotimeUtil;
import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProgrammingServiceImpl implements AutoCloseable, ClusterSingletonService, InstructionScheduler,
        ProgrammingService {
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
    private final RpcProviderRegistry rpcProviderRegistry;
    private RpcRegistration<ProgrammingService> reg;
    private ServiceRegistration<?> serviceRegistration;

    private final class InstructionPusher implements QueueInstruction {
        private final InstructionBuilder builder = new InstructionBuilder();

        InstructionPusher(final InstructionId id, final Nanotime deadline) {
            this.builder.setDeadline(deadline);
            this.builder.setId(id);
            this.builder.setKey(new InstructionKey(id));
            this.builder.setStatus(InstructionStatus.Queued);
        }

        @Override
        public void instructionUpdated(final InstructionStatus status, final Details details) {
            if (!status.equals(this.builder.getStatus())) {
                this.builder.setStatus(status);

                final WriteTransaction wt = ProgrammingServiceImpl.this.dataProvider.newWriteOnlyTransaction();
                wt.put(LogicalDatastoreType.OPERATIONAL,
                        ProgrammingServiceImpl.this.qid.child(
                                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming
                                        .rev150720.instruction.queue.Instruction.class,
                                new InstructionKey(this.builder.getId())), this.builder.build());
                Futures.addCallback(wt.submit(), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        LOG.debug("Instruction Queue {} updated", ProgrammingServiceImpl.this.qid);
                    }

                    @Override
                    public void onFailure(final Throwable trw) {
                        LOG.error("Failed to update Instruction Queue {}", ProgrammingServiceImpl.this.qid, trw);
                    }
                }, MoreExecutors.directExecutor());
            }

            try {
                ProgrammingServiceImpl.this.notifs.putNotification(new InstructionStatusChangedBuilder()
                        .setId(this.builder.getId()).setStatus(status).setDetails(details).build());
            } catch (final InterruptedException e) {
                LOG.debug("Failed to publish notification", e);
            }
        }

        @Override
        public void instructionRemoved() {
            final WriteTransaction wt = ProgrammingServiceImpl.this.dataProvider.newWriteOnlyTransaction();
            wt.delete(LogicalDatastoreType.OPERATIONAL, ProgrammingServiceImpl.this.qid.child(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction
                            .queue.Instruction.class,
                    new InstructionKey(this.builder.getId())));
            Futures.addCallback(wt.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.debug("Instruction Queue {} removed", ProgrammingServiceImpl.this.qid);
                }

                @Override
                public void onFailure(final Throwable trw) {
                    LOG.error("Failed to remove Instruction Queue {}", ProgrammingServiceImpl.this.qid, trw);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    ProgrammingServiceImpl(final DataBroker dataProvider, final NotificationPublishService notifs,
            final ListeningExecutorService executor, final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider cssp, final Timer timer, final String instructionId) {
        this.dataProvider = requireNonNull(dataProvider);
        this.instructionId = requireNonNull(instructionId);
        this.notifs = requireNonNull(notifs);
        this.executor = requireNonNull(executor);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.timer = requireNonNull(timer);
        this.qid = KeyedInstanceIdentifier.builder(InstructionsQueue.class,
                new InstructionsQueueKey(this.instructionId)).build();
        this.sgi = ServiceGroupIdentifier.create(this.instructionId + "-service-group");
        LOG.debug("Creating Programming Service {}.", this.sgi.getValue());
        this.csspReg = cssp.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instruction Queue service {} instantiated", this.sgi.getValue());
        this.reg = this.rpcProviderRegistry.addRpcImplementation(ProgrammingService.class, this);

        final WriteTransaction wt = this.dataProvider.newWriteOnlyTransaction();
        wt.put(LogicalDatastoreType.OPERATIONAL, this.qid, new InstructionsQueueBuilder()
                .setKey(new InstructionsQueueKey(this.instructionId)).setInstruction(Collections.emptyList()).build());
        Futures.addCallback(wt.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Queue {} added", ProgrammingServiceImpl.this.qid);
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed to add Instruction Queue {}", ProgrammingServiceImpl.this.qid, trw);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return this.sgi;
    }

    @Override
    public ListenableFuture<RpcResult<CancelInstructionOutput>> cancelInstruction(final CancelInstructionInput input) {
        return this.executor.submit(() -> realCancelInstruction(input));
    }

    @Override
    public ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanInstructions(final CleanInstructionsInput input) {
        return this.executor.submit(() -> realCleanInstructions(input));
    }

    private synchronized RpcResult<CancelInstructionOutput> realCancelInstruction(final CancelInstructionInput input) {
        final InstructionImpl instruction = this.insns.get(input.getId());
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
        final List<InstructionId> failed = new ArrayList<>();

        for (final InstructionId id : input.getId()) {
            // Find the instruction
            final InstructionImpl instruction = this.insns.get(id);
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

            this.insns.remove(id);
            LOG.debug("Instruction {} cleaned successfully", id);
        }

        final CleanInstructionsOutputBuilder ob = new CleanInstructionsOutputBuilder();
        ob.setUnflushed(failed);

        return SuccessfulRpcResult.create(ob.build());
    }

    private List<InstructionImpl> checkDependencies(final SubmitInstructionInput input) throws SchedulerException {
        final List<InstructionImpl> dependencies = collectDependencies(input);
        // Check if all dependencies are non-failed
        final List<InstructionId> unmet = checkIfUnfailed(dependencies);
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
            final InstructionImpl instruction = this.insns.get(pid);
            if (instruction == null) {
                LOG.info("Instruction {} depends on {}, which is not a known instruction", input.getId(), pid);
                throw new SchedulerException("Unknown dependency ID specified",
                        new FailureBuilder().setType(UnknownPreconditionId.class).build());
            }
            dependencies.add(instruction);
        }
        return dependencies;
    }

    private static List<InstructionId> checkIfUnfailed(final List<InstructionImpl> dependencies) {
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
        if (this.insns.get(id) != null) {
            LOG.info("Instruction ID {} already present", id);
            throw new SchedulerException("Instruction ID currently in use",
                    new FailureBuilder().setType(DuplicateInstructionId.class).build());
        }

        // First things first: check the deadline
        final Nanotime now = NanotimeUtil.currentTime();
        final BigInteger left = input.getDeadline().getValue().subtract(now.getValue());

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
        final Timeout t = this.timer.newTimeout(timeout -> timeoutInstruction(input.getId()), left.longValue(),
                TimeUnit.NANOSECONDS);

        // Put it into the instruction list
        final SettableFuture<Instruction> ret = SettableFuture.create();
        final InstructionImpl instruction = new InstructionImpl(new InstructionPusher(id, input.getDeadline()), ret, id,
                dependencies, t);
        this.insns.put(id, instruction);

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
        this.executor.submit(() -> tryScheduleInstruction(instruction));

        return ret;
    }

    @Override
    public String getInstructionID() {
        return this.instructionId;
    }

    private synchronized void timeoutInstruction(final InstructionId id) {
        final InstructionImpl instruction = this.insns.get(id);
        if (instruction == null) {
            LOG.warn("Instruction {} timed out, but not found in the queue", id);
            return;
        }

        instruction.timeout();
    }

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
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Closing Instruction Queue service {}", this.sgi.getValue());

        this.reg.close();
        for (final InstructionImpl instruction : this.insns.values()) {
            instruction.tryCancel(null);
        }
        // Workaround for BUG-2283
        final WriteTransaction wt = this.dataProvider.newWriteOnlyTransaction();
        wt.delete(LogicalDatastoreType.OPERATIONAL, this.qid);

        final ListenableFuture<Void> future = wt.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Queue {} removed", ProgrammingServiceImpl.this.qid);
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed to shutdown Instruction Queue {}", ProgrammingServiceImpl.this.qid, trw);
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public synchronized void close() {
        if (this.csspReg != null) {
            try {
                this.csspReg.close();
            } catch (final Exception e) {
                LOG.error("Failed to close Instruction Scheduler service", e);
            }
        }
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
    }

    void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }
}
