/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CancelInstructionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.CleanInstructionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatusChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.Nanotime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ProgrammingServiceImplTest {

	private ProgrammingServiceImpl testedProgrammingService;

	@Mock
	private DataProviderService mockedDataProvider;
	@Mock
	private NotificationProviderService mockedNotificationService;
	@Mock
	private ListeningExecutorService mockedExecutor;
	@Mock
	private DataModificationTransaction mockedTransaction;
	private Timer timer = new HashedWheelTimer();

	private List<Object> submittedTasksToExecutor;
	private List<Notification> publishedNotifications;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		submittedTasksToExecutor = Lists.newArrayList();
		publishedNotifications = Lists.newArrayList();

		testedProgrammingService = new ProgrammingServiceImpl(getMockedDataProvider(), getMockedNotificationService(),
				getMockedExecutor(), getTimer());

		verify(mockedDataProvider).beginTransaction();
		verify(mockedTransaction).putOperationalData(Matchers.<InstanceIdentifier<? extends DataObject>>any(),
				any(DataObject.class));
		verify(mockedTransaction).commit();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testScheduleInstruction() throws Exception {
		SubmitInstructionInput mockedSubmit = getMockedSubmitInstructionInput("mockedSubmit");
		testedProgrammingService.scheduleInstruction(mockedSubmit);

		// assert Schedule to executor
		assertEquals(1, submittedTasksToExecutor.size());

		// assert Notification
		assertEquals(1, publishedNotifications.size());
		assertTrue(InstructionStatusChanged.class.isAssignableFrom(publishedNotifications.get(0).getClass()));
		InstructionStatusChanged firstNotification = (InstructionStatusChanged) publishedNotifications.get(0);
		assertInstructionStatusChangedNotification(mockedSubmit.getId(), InstructionStatus.Scheduled, firstNotification);

		verify(mockedDataProvider, times(2)).beginTransaction();
		verify(mockedTransaction, times(2)).commit();
	}

	private void assertInstructionStatusChangedNotification(InstructionId id, InstructionStatus status, InstructionStatusChanged firstNotification) {
		Assert.assertEquals(id, firstNotification.getId());
		Assert.assertEquals(status, firstNotification.getStatus());
	}

	@Test
	public void testScheduleDependingInstruction() throws Exception {
		testedProgrammingService.scheduleInstruction(getMockedSubmitInstructionInput("mockedSubmit1"));
		SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit2);

		assertEquals(2, submittedTasksToExecutor.size());

		// First is in state scheduled, so second could not be scheduled yet
		assertEquals(1, publishedNotifications.size());
	}

	@Test
	public void testScheduleDependingInstructionToFail() throws Exception {
		try {
			testedProgrammingService.scheduleInstruction(getMockedSubmitInstructionInput("mockedSubmit", "dep1"));
		} catch (SchedulerException e) {
			assertThat(e.getMessage(), JUnitMatchers.containsString("Unknown dependency ID"));
			verifyZeroInteractions(mockedNotificationService);
			return;
		}
		fail("Instruction schedule should fail on unresolved dependencies");
	}

	@Test
	public void testCancelInstruction() throws Exception {
		SubmitInstructionInput mockedSubmit = getMockedSubmitInstructionInput("mockedSubmit");
		testedProgrammingService.scheduleInstruction(mockedSubmit);

		testedProgrammingService.cancelInstruction(getCancelInstruction("mockedSubmit"));

		assertEquals(2, submittedTasksToExecutor.size());

		assertEquals(2, publishedNotifications.size());
		assertTrue(InstructionStatusChanged.class.isAssignableFrom(publishedNotifications.get(1).getClass()));
		InstructionStatusChanged cancelNotification = (InstructionStatusChanged) publishedNotifications.get(1);
		assertInstructionStatusChangedNotification(mockedSubmit.getId(), InstructionStatus.Cancelled, cancelNotification);
	}

	private CancelInstructionInput getCancelInstruction(String instructionId) {
		CancelInstructionInputBuilder builder = new CancelInstructionInputBuilder();
		builder.setId(getInstructionId(instructionId));
		return builder.build();
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

		assertEquals(1 /*First Scheduled*/ + 3 /*First and all dependencies cancelled*/,
				publishedNotifications.size());

		assertInstructionStatusChangedNotification(mockedSubmit1.getId(), InstructionStatus.Scheduled,
				(InstructionStatusChanged) publishedNotifications.get(0));
		assertInstructionStatusChangedNotification(mockedSubmit1.getId(), InstructionStatus.Cancelled,
				(InstructionStatusChanged) publishedNotifications.get(1));
		assertInstructionStatusChangedNotification(mockedSubmit2.getId(), InstructionStatus.Cancelled,
				(InstructionStatusChanged) publishedNotifications.get(2));
		assertInstructionStatusChangedNotification(mockedSubmit3.getId(), InstructionStatus.Cancelled,
				(InstructionStatusChanged) publishedNotifications.get(3));
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
		Assert.assertEquals(2, cleanedInstructionOutput.get().getResult().getUnflushed().size());
		Assert.assertEquals(0, cleanedInstructionOutput.get().getErrors().size());

		testedProgrammingService.cancelInstruction(getCancelInstruction("mockedSubmit1"));

		cleanedInstructionOutput = testedProgrammingService.cleanInstructions(cleanInstructionsInput);
		Assert.assertEquals(0, cleanedInstructionOutput.get().getResult().getUnflushed().size());
		Assert.assertEquals(0, cleanedInstructionOutput.get().getErrors().size());
	}

	@Test
	public void testCloseProgrammingService() throws Exception {
		SubmitInstructionInput mockedSubmit1 = getMockedSubmitInstructionInput("mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit1);
		SubmitInstructionInput mockedSubmit2 = getMockedSubmitInstructionInput("mockedSubmit2", "mockedSubmit1");
		testedProgrammingService.scheduleInstruction(mockedSubmit2);

		testedProgrammingService.close();

		assertEquals(1/* First scheduled */+ 2/* Both cancelled at close */, publishedNotifications.size());
		verify(mockedTransaction).removeOperationalData(Matchers.<InstanceIdentifier<? extends DataObject>> any());
	}

	private NotificationProviderService getMockedNotificationService() {
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object notif = invocation.getArguments()[0];
				assertTrue(Notification.class.isAssignableFrom(notif.getClass()));
				publishedNotifications.add((Notification) notif);
				return null;
			}
		}).when(mockedNotificationService).publish(any(Notification.class));
		return mockedNotificationService;
	}

	private DataProviderService getMockedDataProvider() {
		doReturn(null).when(mockedTransaction).readOperationalData(
				Matchers.<InstanceIdentifier<? extends DataObject>> any());
		doReturn(mockedTransaction).when(mockedDataProvider).beginTransaction();
		doNothing().when(mockedTransaction).putOperationalData(
				Matchers.<InstanceIdentifier<? extends DataObject>> any(), any(DataObject.class));
		Future mockedCommitFuture = mock(Future.class);
		doNothing().when(mockedTransaction).removeOperationalData(
				Matchers.<InstanceIdentifier<? extends DataObject>> any());
		doReturn(mockedCommitFuture).when(mockedTransaction).commit();
		return mockedDataProvider;
	}

	private ListeningExecutorService getMockedExecutor() {
		ListeningExecutorService mockedExecutor = mock(ListeningExecutorService.class);
		Answer<ListenableFuture<?>> submitAnswer = new Answer<ListenableFuture<?>>() {
			@Override
			public ListenableFuture<?> answer(InvocationOnMock invocation) throws Throwable {
				Object task = invocation.getArguments()[0];
				submittedTasksToExecutor.add(task);

				Object result = null;
				if(task instanceof Runnable) {
					((Runnable)task).run();
				} else if(task instanceof Callable) {
					result = ((Callable<?>)task).call();
				}

				ListenableFuture<?> mockedFuture = mock(ListenableFuture.class);
				doReturn(result).when(mockedFuture).get();
				return mockedFuture;
			}
		};
		doAnswer(submitAnswer).when(mockedExecutor).submit(any(Runnable.class));
		doAnswer(submitAnswer).when(mockedExecutor).submit(any(Callable.class));
		return mockedExecutor;
	}

	private Timer getTimer() {
		return timer;
	}

	private SubmitInstructionInput getMockedSubmitInstructionInput(String id, String... dependencyIds) {
		SubmitInstructionInput mockedSubmitInstruction = mock(SubmitInstructionInput.class);

		doReturn(PcepUpdateTunnelInput.class).when(mockedSubmitInstruction).getImplementedInterface();
		List<InstructionId> dependencies = Lists.newArrayList();
		for (String dependencyId : dependencyIds) {
			dependencies.add(getInstructionId(dependencyId));
		}

		doReturn(dependencies).when(mockedSubmitInstruction).getPreconditions();
		doReturn(getInstructionId(id)).when(mockedSubmitInstruction).getId();
		doReturn(new Nanotime(BigInteger.valueOf(Long.MAX_VALUE))).when(mockedSubmitInstruction).getDeadline();
		return mockedSubmitInstruction;
	}

	private InstructionId getInstructionId(String id) {
		return new InstructionId(id);
	}
}
