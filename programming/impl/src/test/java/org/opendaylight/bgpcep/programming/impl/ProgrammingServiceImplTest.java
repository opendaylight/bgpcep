/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.opendaylight.bgpcep.programming.NanotimeUtil;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionQueue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.Nanotime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.DetailsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ProgrammingServiceImplTest {

    public static final int INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS = 3;

    private ProgrammingServiceImpl testedProgrammingService;
    private MockedExecutorWrapper mockedExecutorWrapper;
    private MockedDataProviderWrapper mockedDataProviderWrapper;
    private MockedNotificationServiceWrapper mockedNotificationServiceWrapper;
	private Timer timer = new HashedWheelTimer();

    @Before
    public void setUp() throws Exception {
        mockedDataProviderWrapper = new MockedDataProviderWrapper();
        mockedExecutorWrapper = new MockedExecutorWrapper();
        mockedNotificationServiceWrapper = new MockedNotificationServiceWrapper();

        testedProgrammingService = new ProgrammingServiceImpl(mockedDataProviderWrapper.getMockedDataProvider(),
                mockedNotificationServiceWrapper.getMockedNotificationService(),
                mockedExecutorWrapper.getMockedExecutor(), timer);

        mockedDataProviderWrapper.verifyBeginTransaction(1).verifyPutDataOnTransaction(1).verifyCommitTransaction(1);
        mockedDataProviderWrapper.assertPutDataTargetType(0, InstructionQueue.class);
    }

    @After
	public void tearDown() throws Exception {
	}

    @Test
    public void testScheduleInstruction() throws Exception {
        SubmitInstructionInput mockedSubmit = getMockedSubmitInstructionInput("mockedSubmit");
        testedProgrammingService.scheduleInstruction(mockedSubmit);

        // assert Schedule to executor
        mockedExecutorWrapper.assertSubmittedTasksSize(1);

        // assert Notification
        mockedNotificationServiceWrapper.assertNotificationsCount(1);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(0, mockedSubmit.getId(),
                InstructionStatus.Scheduled);

        mockedDataProviderWrapper.verifyBeginTransaction(2).verifyPutDataOnTransaction(2).verifyCommitTransaction(2)
                .verifyRemoveDataOnTransaction(0);
        mockedDataProviderWrapper.assertPutDataForInstructions(1, mockedSubmit.getId(), mockedSubmit.getDeadline());
    }

	@Test
	public void testScheduleDependingInstruction() throws Exception {
		testedProgrammingService.scheduleInstruction(getMockedSubmitInstructionInput("mockedSubmit1"));
		SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit2);

        mockedExecutorWrapper.assertSubmittedTasksSize(2);

		// First is in state scheduled, so second could not be scheduled yet
	    mockedNotificationServiceWrapper.assertNotificationsCount(1);
    }

	@Test
	public void testScheduleDependingInstructionToFail() throws Exception {
		try {
			testedProgrammingService.scheduleInstruction(getMockedSubmitInstructionInput("mockedSubmit", "dep1"));
		} catch (SchedulerException e) {
			assertThat(e.getMessage(), JUnitMatchers.containsString("Unknown dependency ID"));
            mockedNotificationServiceWrapper.assertNotificationsCount(0);
            return;
		}
		fail("Instruction schedule should fail on unresolved dependencies");
	}

    @Test
    public void testCancelInstruction() throws Exception {
        SubmitInstructionInput mockedSubmit = getMockedSubmitInstructionInput("mockedSubmit");
        testedProgrammingService.scheduleInstruction(mockedSubmit);

        CancelInstructionInput mockedCancel = getCancelInstruction("mockedSubmit");
        testedProgrammingService.cancelInstruction(mockedCancel);

        mockedExecutorWrapper.assertSubmittedTasksSize(2);

        mockedNotificationServiceWrapper.assertNotificationsCount(2);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit.getId(),
                InstructionStatus.Cancelled);

        mockedDataProviderWrapper.verifyBeginTransaction(2/*init + schedule first*/ + 1 /*cancel*/).
                verifyPutDataOnTransaction(3).verifyRemoveDataOnTransaction(0).verifyCommitTransaction(3);

        mockedDataProviderWrapper.assertPutDataForInstructions(1, mockedSubmit.getId(), mockedSubmit.getDeadline());

        mockedDataProviderWrapper.assertPutDataForInstructions(2, mockedCancel.getId(), null);
    }

    @Test
	public void testCancelDependantInstruction() throws Exception {
		SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit1);
		SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit2);
		SubmitInstructionInput mockedSubmit3 = getMockedSubmitInstructionInput("mockedSubmit3", "mockedSubmit1",
				"mockedSubmit2");
		testedProgrammingService.scheduleInstruction(mockedSubmit3);

		testedProgrammingService.cancelInstruction(getCancelInstruction("mockedSubmit1"));

        mockedNotificationServiceWrapper.assertNotificationsCount(1 /*First Scheduled*/ + 3 /*First and all dependencies cancelled*/);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(0, mockedSubmit1.getId(),
                InstructionStatus.Scheduled);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(),
                InstructionStatus.Cancelled);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit2.getId(),
                InstructionStatus.Cancelled);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(3, mockedSubmit3.getId(),
                InstructionStatus.Cancelled);
	}

	@Test
	public void testCleanInstructions() throws Exception {
        SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit1);
		SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit2);

		CleanInstructionsInputBuilder cleanInstructionsInputBuilder = new CleanInstructionsInputBuilder();
		CleanInstructionsInput cleanInstructionsInput = cleanInstructionsInputBuilder.setId(
				Lists.newArrayList(mockedSubmit1.getId(), mockedSubmit2.getId())).build();

		ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanedInstructionOutput = testedProgrammingService
				.cleanInstructions(cleanInstructionsInput);

        assertCleanInstructionOutput(cleanedInstructionOutput, 2);

        int expectedBeginTxCount = 1/*Service init*/ + 1;
        mockedDataProviderWrapper.verifyBeginTransaction(expectedBeginTxCount/*Schedule first*/).
                verifyCommitTransaction(expectedBeginTxCount)
                .verifyPutDataOnTransaction(expectedBeginTxCount)
                .verifyRemoveDataOnTransaction(0);

		testedProgrammingService.cancelInstruction(getCancelInstruction("mockedSubmit1"));

		cleanedInstructionOutput = testedProgrammingService.cleanInstructions(cleanInstructionsInput);
        assertCleanInstructionOutput(cleanedInstructionOutput, 0);
        mockedDataProviderWrapper.verifyBeginTransaction(expectedBeginTxCount + 2/*Update status to cancelled*/ + 2 /*Cleanup*/)
                .verifyRemoveDataOnTransaction(2)
                .verifyPutDataOnTransaction(2/*From before*/ + 2/*Cancel*/)
                .verifyCommitTransaction(expectedBeginTxCount + 2/*Update status to cancelled*/ + 2 /*Cleanup*/);

        mockedDataProviderWrapper.assertRemoveDataForInstruction(0, mockedSubmit1.getId());
        mockedDataProviderWrapper.assertRemoveDataForInstruction(1, mockedSubmit2.getId());
	}

    private void assertCleanInstructionOutput(
            ListenableFuture<RpcResult<CleanInstructionsOutput>> cleanedInstructionOutput, int unflushedCount)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        Assert.assertEquals(unflushedCount, cleanedInstructionOutput.get().getResult().getUnflushed().size());
        Assert.assertEquals(0, cleanedInstructionOutput.get().getErrors().size());
    }

    @Test
	public void testCloseProgrammingService() throws Exception {
		SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit1);
		SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit2);

		testedProgrammingService.close();

        mockedNotificationServiceWrapper.assertNotificationsCount(1/* First scheduled */ + 2/* Both cancelled at close */);
        mockedDataProviderWrapper.verifyRemoveDataOnTransaction(1);
	}

    @Test(timeout = 30 * 1000)
    public void testTimeoutWhileScheduledTransaction() throws Exception {
        BigInteger deadlineOffset = BigInteger.valueOf(1000l * 1000 * 1000 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS /* seconds */);
        Nanotime current = NanotimeUtil.currentTime();
        Nanotime deadlineNano = new Nanotime(current.getValue().add(deadlineOffset));

        Optional<Nanotime> deadline = Optional.of(deadlineNano);
        SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1", deadline);
        ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        future.get();

        Thread.sleep(2 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS * 1000);

        mockedNotificationServiceWrapper.assertNotificationsCount(2);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(),
                InstructionStatus.Cancelled);
    }

    @Test(timeout = 30 * 1000)
    public void testTimeoutWhileSuccessfulTransaction() throws Exception {
        BigInteger deadlineOffset = BigInteger.valueOf(1000l * 1000 * 1000 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS /* seconds */);
        Nanotime current = NanotimeUtil.currentTime();
        Nanotime deadlineNano = new Nanotime(current.getValue().add(deadlineOffset));

        Optional<Nanotime> deadline = Optional.of(deadlineNano);
        SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1", deadline);
        ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        Instruction i = future.get();
        i.checkedExecutionStart();
        i.executionCompleted(InstructionStatus.Successful, getDetails());

        Thread.sleep(2 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS * 1000);

        mockedNotificationServiceWrapper.assertNotificationsCount(3);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(),
                InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit1.getId(),
                InstructionStatus.Successful);
        // Timeout in success should not do anything
    }

    @Test(timeout = 30 * 1000)
    public void testTimeoutWhileExecutingWithDependenciesTransaction() throws Exception {
        BigInteger deadlineOffset = BigInteger.valueOf(1000l * 1000 * 1000 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS /* seconds */);
        Nanotime current = NanotimeUtil.currentTime();
        Nanotime deadlineNano = new Nanotime(current.getValue().add(deadlineOffset));

        Optional<Nanotime> deadline = Optional.of(deadlineNano);
        SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1", deadline);
        ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        testedProgrammingService.scheduleInstruction(mockedSubmit2);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        Instruction i = future.get();
        i.checkedExecutionStart();

        Thread.sleep(2 * INSTRUCTION_DEADLINE_OFFSET_IN_SECONDS * 1000);

        mockedNotificationServiceWrapper.assertNotificationsCount(4);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(),
                InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit1.getId(),
                InstructionStatus.Unknown);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(3, mockedSubmit2.getId(),
                InstructionStatus.Cancelled);
    }

    // TODO test deadline with state Queued

    @Test
    public void testSuccessExecutingWithDependenciesTransaction() throws Exception {
        SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
        ListenableFuture<Instruction> future = testedProgrammingService.scheduleInstruction(mockedSubmit1);

        SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
        ListenableFuture<Instruction> future2 = testedProgrammingService.scheduleInstruction(mockedSubmit2);

        mockedNotificationServiceWrapper.assertNotificationsCount(1);

        Instruction i = future.get();
        i.checkedExecutionStart();
        i.executionCompleted(InstructionStatus.Successful, getDetails());


        mockedNotificationServiceWrapper.assertNotificationsCount(4);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(1, mockedSubmit1.getId(),
                InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(2, mockedSubmit1.getId(),
                InstructionStatus.Successful);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(3, mockedSubmit2.getId(),
                InstructionStatus.Scheduled);

        i = future2.get();
        i.checkedExecutionStart();
        i.executionCompleted(InstructionStatus.Successful, getDetails());

        mockedNotificationServiceWrapper.assertNotificationsCount(6);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(4, mockedSubmit2.getId(),
                InstructionStatus.Executing);
        mockedNotificationServiceWrapper.assertInstructionStatusChangedNotification(5, mockedSubmit2.getId(),
                InstructionStatus.Successful);
    }

    private Details getDetails() {
        return new DetailsBuilder().build();
    }

    private SubmitInstructionInput getMockedSubmitInstructionInput(String id, String... dependencyIds) {
        return getMockedSubmitInstructionInput(id, Optional.<Nanotime>absent(), dependencyIds);
    }

    private SubmitInstructionInput getMockedSubmitInstructionInput(String id, Optional<Nanotime> deadline, String... dependencyIds) {
		SubmitInstructionInput mockedSubmitInstruction = mock(SubmitInstructionInput.class);

		doReturn(PcepUpdateTunnelInput.class).when(mockedSubmitInstruction).getImplementedInterface();
		List<InstructionId> dependencies = Lists.newArrayList();
		for (String dependencyId : dependencyIds) {
			dependencies.add(getInstructionId(dependencyId));
		}

		doReturn(dependencies).when(mockedSubmitInstruction).getPreconditions();
		doReturn(getInstructionId(id)).when(mockedSubmitInstruction).getId();
        doReturn(deadline.isPresent() ? deadline.get() : new Nanotime(BigInteger.valueOf(Long.MAX_VALUE))).when(
                mockedSubmitInstruction).getDeadline();
		return mockedSubmitInstruction;
	}

    private CancelInstructionInput getCancelInstruction(String instructionId) {
        CancelInstructionInputBuilder builder = new CancelInstructionInputBuilder();
        builder.setId(getInstructionId(instructionId));
        return builder.build();
    }

	private InstructionId getInstructionId(String id) {
		return new InstructionId(id);
	}

}
