/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.programming.NanotimeUtil;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CancelInstructionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CleanInstructionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CleanInstructionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.CleanInstructionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionsQueue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionsQueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.Nanotime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.queue.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.DetailsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class ProgrammingServiceImplTest extends AbstractDataBrokerTest {

    private static final int INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS = 3;
    private static final InstructionsQueueKey INSTRUCTIONS_QUEUE_KEY = new InstructionsQueueKey("test-instraction-queue");
    private final Timer timer = new HashedWheelTimer();
    private MockedExecutorWrapper mockedExecutorWrapper;
    private MockedNotificationServiceWrapper mockedNotificationServiceWrapper;
    private ProgrammingServiceImpl testedProgrammingService;
    @Mock
    private ClusterSingletonServiceProvider cssp;
    @Mock
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    @Mock
    private RpcProviderRegistry rpcRegistry;
    @Mock
    private RoutedRpcRegistration<ProgrammingService> registration;
    private ClusterSingletonService singletonService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doAnswer(invocationOnMock -> {
            this.singletonService = (ClusterSingletonService) invocationOnMock.getArguments()[0];
            return this.singletonServiceRegistration;
        }).when(this.cssp).registerClusterSingletonService(any(ClusterSingletonService.class));

        doAnswer(invocationOnMock -> {
            this.singletonService.closeServiceInstance();
            return null;
        }).when(this.singletonServiceRegistration).close();
        doReturn(this.registration).when(this.rpcRegistry).addRpcImplementation(Mockito.any(),
            Mockito.any(ProgrammingService.class));
        doNothing().when(this.registration).close();
        this.mockedExecutorWrapper = new MockedExecutorWrapper();
        this.mockedNotificationServiceWrapper = new MockedNotificationServiceWrapper();

        this.testedProgrammingService = new ProgrammingServiceImpl(getDataBroker(),
            this.mockedNotificationServiceWrapper.getMockedNotificationService(),
            this.mockedExecutorWrapper.getMockedExecutor(), this.rpcRegistry, this.cssp, this.timer, INSTRUCTIONS_QUEUE_KEY);
        this.singletonService.instantiateServiceInstance();
    }

    @After
    public void tearDown() throws Exception {
        this.singletonService.closeServiceInstance();
        this.testedProgrammingService.close();
    }

    @Test
    public void testScheduleInstruction() throws Exception {
        final SubmitInstructionInput mockedSubmit = getMockedSubmitInstructionInput("mockedSubmit");
        testedProgrammingService.scheduleInstruction(mockedSubmit);

        assertTrue(assertInstructionExists(mockedSubmit.getId()));

        // assert Schedule to executor
        mockedExecutorWrapper.assertSubmittedTasksSize(1);

        // assert Notification
        mockedNotificationServiceWrapper.assertNotificationsCount(1);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(0, mockedSubmit.getId(), InstructionStatus.Scheduled);
    }

    @Test
    public void testScheduleDependingInstruction() throws Exception {
        testedProgrammingService.scheduleInstruction(getMockedSubmitInstructionInput("mockedSubmit1"));
        final SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit2);

        mockedExecutorWrapper.assertSubmittedTasksSize(2);

        // First is in state scheduled, so second could not be scheduled yet
        mockedNotificationServiceWrapper.assertNotificationsCount(1);
    }

    @Test
    public void testScheduleDependingInstructionToFail() throws Exception {
        try {
            testedProgrammingService.scheduleInstruction(getMockedSubmitInstructionInput("mockedSubmit", "dep1"));
        } catch (final SchedulerException e) {
            assertThat(e.getMessage(), containsString("Unknown dependency ID"));
            mockedNotificationServiceWrapper.assertNotificationsCount(0);
            return;
        }
        fail("Instruction schedule should fail on unresolved dependencies");
    }

    @Test
    public void testCancelInstruction() throws Exception {
        final SubmitInstructionInput mockedSubmit = getMockedSubmitInstructionInput("mockedSubmit");
        testedProgrammingService.scheduleInstruction(mockedSubmit);

        assertTrue(assertInstructionExists(mockedSubmit.getId()));

        final CancelInstructionInput mockedCancel = getCancelInstruction("mockedSubmit");
        testedProgrammingService.cancelInstruction(mockedCancel);

        assertTrue(assertInstructionExists(mockedSubmit.getId()));

        mockedExecutorWrapper.assertSubmittedTasksSize(2);

        mockedNotificationServiceWrapper.assertNotificationsCount(2);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit.getId(), InstructionStatus.Cancelled);
    }

    @Test
    public void testCancelDependantInstruction() throws Exception {
        final SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit1);
        final SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit2);
        final SubmitInstructionInput mockedSubmit3 = getMockedSubmitInstructionInput("mockedSubmit3", "mockedSubmit1", "mockedSubmit2");
        testedProgrammingService.scheduleInstruction(mockedSubmit3);

        testedProgrammingService.cancelInstruction(getCancelInstruction("mockedSubmit1"));

        mockedNotificationServiceWrapper.assertNotificationsCount(1 /*First Scheduled*/+ 3 /*First and all dependencies cancelled*/);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(0, mockedSubmit1.getId(), InstructionStatus.Scheduled);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(), InstructionStatus.Cancelled);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit2.getId(), InstructionStatus.Cancelled);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(3, mockedSubmit3.getId(), InstructionStatus.Cancelled);

        assertTrue(assertInstructionExists(mockedSubmit1.getId()));
        assertTrue(assertInstructionExists(mockedSubmit2.getId()));
        assertTrue(assertInstructionExists(mockedSubmit3.getId()));
    }

    @Test
    public void testCleanInstructions() throws Exception {
        final SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit1);
        final SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit2);

        final CleanInstructionsInputBuilder cleanInstructionsInputBuilder = new CleanInstructionsInputBuilder();
        final CleanInstructionsInput cleanInstructionsInput = cleanInstructionsInputBuilder.setId(
                Lists.newArrayList(mockedSubmit1.getId(), mockedSubmit2.getId())).build();

        ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanedInstructionOutput = testedProgrammingService.cleanInstructions(cleanInstructionsInput);

        assertCleanInstructionOutput(cleanedInstructionOutput, 2);

        testedProgrammingService.cancelInstruction(getCancelInstruction("mockedSubmit1"));

        cleanedInstructionOutput = testedProgrammingService.cleanInstructions(cleanInstructionsInput);
        assertCleanInstructionOutput(cleanedInstructionOutput, 0);

        assertFalse(assertInstructionExists(mockedSubmit1.getId()));
        assertFalse(assertInstructionExists(mockedSubmit2.getId()));
    }

    private void assertCleanInstructionOutput(final ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanedInstructionOutput,
            final int unflushedCount) throws InterruptedException, java.util.concurrent.ExecutionException {
        if (unflushedCount == 0) {
            final List<InstructionId> unflushed = cleanedInstructionOutput.get().getResult().getUnflushed();
            assertTrue(unflushed == null || unflushed.isEmpty());
        } else {
            assertEquals(unflushedCount, cleanedInstructionOutput.get().getResult().getUnflushed().size());
        }
        assertEquals(0, cleanedInstructionOutput.get().getErrors().size());
    }

    @Test
    public void testCloseProgrammingService() throws Exception {
        final SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit1);
        final SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit2);

        testedProgrammingService.close();

        mockedNotificationServiceWrapper.assertNotificationsCount(1/* First scheduled */+ 2/* Both cancelled at close */);
    }

    @Test(timeout = 30 * 1000)
    public void testTimeoutWhileScheduledTransaction() throws Exception {
        final BigInteger deadlineOffset = BigInteger.valueOf(1000L * 1000 * 1000 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS /* seconds */);
        final Nanotime current = NanotimeUtil.currentTime();
        final Nanotime deadlineNano = new Nanotime(current.getValue().add(deadlineOffset));

        final Optional<Nanotime> deadline = Optional.of(deadlineNano);
        final SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1", deadline);
        final ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        future.get();

        Thread.sleep(2 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS * 1000);

        mockedNotificationServiceWrapper.assertNotificationsCount(2);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(), InstructionStatus.Cancelled);
    }

    @Test(timeout = 30 * 1000)
    public void testTimeoutWhileSuccessfulTransaction() throws Exception {
        final BigInteger deadlineOffset = BigInteger.valueOf(1000L * 1000 * 1000 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS /* seconds */);
        final Nanotime current = NanotimeUtil.currentTime();
        final Nanotime deadlineNano = new Nanotime(current.getValue().add(deadlineOffset));

        final Optional<Nanotime> deadline = Optional.of(deadlineNano);
        final SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1", deadline);
        final ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        final Instruction i = future.get();
        i.checkedExecutionStart();
        i.executionCompleted(InstructionStatus.Successful, getDetails());

        Thread.sleep(2 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS * 1000);

        mockedNotificationServiceWrapper.assertNotificationsCount(3);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(), InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit1.getId(), InstructionStatus.Successful);
        // Timeout in success should not do anything
    }

    @Test(timeout = 30 * 1000)
    public void testTimeoutWhileExecutingWithDependenciesTransaction() throws Exception {
        final BigInteger deadlineOffset = BigInteger.valueOf(1000L * 1000 * 1000 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS /* seconds */);
        final Nanotime current = NanotimeUtil.currentTime();
        final Nanotime deadlineNano = new Nanotime(current.getValue().add(deadlineOffset));

        final Optional<Nanotime> deadline = Optional.of(deadlineNano);
        final SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1", deadline);
        final ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        final SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit2);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        final Instruction i = future.get();
        i.checkedExecutionStart();

        Thread.sleep(2 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS * 1000);

        mockedNotificationServiceWrapper.assertNotificationsCount(4);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(), InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit1.getId(), InstructionStatus.Unknown);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(3, mockedSubmit2.getId(), InstructionStatus.Cancelled);
    }

    // TODO test deadline with state Queued

    @Test
    public void testSuccessExecutingWithDependenciesTransaction() throws Exception {
        final SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
        final ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        final SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        final ListenableFuture<Instruction> future2 = testedProgrammingService.scheduleInstruction(mockedSubmit2);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        Instruction i = future.get();
        i.checkedExecutionStart();
        i.executionCompleted(InstructionStatus.Successful, getDetails());

        mockedNotificationServiceWrapper.assertNotificationsCount(4);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(), InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit1.getId(), InstructionStatus.Successful);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(3, mockedSubmit2.getId(), InstructionStatus.Scheduled);

        i = future2.get();
        i.checkedExecutionStart();
        i.executionCompleted(InstructionStatus.Successful, getDetails());

        mockedNotificationServiceWrapper.assertNotificationsCount(6);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(4, mockedSubmit2.getId(), InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(5, mockedSubmit2.getId(), InstructionStatus.Successful);
    }

    private Details getDetails() {
        return new DetailsBuilder().build();
    }

    private SubmitInstructionInput getMockedSubmitInstructionInput(final String id, final String... dependencyIds) {
        return getMockedSubmitInstructionInput(id, Optional.empty(), dependencyIds);
    }

    private SubmitInstructionInput getMockedSubmitInstructionInput(final String id, final Optional<Nanotime> deadline, final String... dependencyIds) {
        final SubmitInstructionInput mockedSubmitInstruction = mock(SubmitInstructionInput.class);

        doReturn(PcepUpdateTunnelInput.class).when(mockedSubmitInstruction).getImplementedInterface();
        final List<InstructionId> dependencies = Lists.newArrayList();
        for (final String dependencyId : dependencyIds) {
            dependencies.add(getInstructionId(dependencyId));
        }

        doReturn(dependencies).when(mockedSubmitInstruction).getPreconditions();
        doReturn(getInstructionId(id)).when(mockedSubmitInstruction).getId();
        doReturn(deadline.isPresent() ? deadline.get() : new Nanotime(BigInteger.valueOf(Long.MAX_VALUE))).when(mockedSubmitInstruction).getDeadline();
        return mockedSubmitInstruction;
    }

    private CancelInstructionInput getCancelInstruction(final String instructionId) {
        final CancelInstructionInputBuilder builder = new CancelInstructionInputBuilder();
        builder.setId(getInstructionId(instructionId));
        return builder.build();
    }

    private InstructionId getInstructionId(final String id) {
        return new InstructionId(id);
    }

    private boolean assertInstructionExists(final InstructionId id) {
        try {
            return getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(InstructionsQueue.class, INSTRUCTIONS_QUEUE_KEY).build().child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.queue.Instruction.class,
                    new InstructionKey(id))).get().isPresent();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }
}
