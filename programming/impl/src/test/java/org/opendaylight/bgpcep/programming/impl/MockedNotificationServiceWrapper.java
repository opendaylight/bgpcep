/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.List;

import junit.framework.Assert;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatusChanged;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Lists;

final class MockedNotificationServiceWrapper {
	private final List<Notification> publishedNotifications;

	MockedNotificationServiceWrapper() {
		this.publishedNotifications = Lists.newArrayList();
	}

	NotificationProviderService getMockedNotificationService() {
		final NotificationProviderService mockedNotificationService = mock(NotificationProviderService.class);

		doAnswer(new Answer() {
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final Object notif = invocation.getArguments()[0];
				assertTrue(Notification.class.isAssignableFrom(notif.getClass()));
				MockedNotificationServiceWrapper.this.publishedNotifications.add((Notification) notif);
				return null;
			}
		}).when(mockedNotificationService).publish(any(Notification.class));
		return mockedNotificationService;
	}

	void assertNotificationsCount(final int count) {
		assertEquals(count, this.publishedNotifications.size());
	}

	public void assertInstructionStatusChangedNotification(final int idx, final InstructionId id, final InstructionStatus status) {
		assertTrue(InstructionStatusChanged.class.isAssignableFrom(this.publishedNotifications.get(idx).getClass()));
		final InstructionStatusChanged firstNotification = (InstructionStatusChanged) this.publishedNotifications.get(idx);
		assertInstructionStatusChangedNotification(id, status, firstNotification);
	}

	private void assertInstructionStatusChangedNotification(final InstructionId id, final InstructionStatus status,
			final InstructionStatusChanged firstNotification) {
		Assert.assertEquals(id, firstNotification.getId());
		Assert.assertEquals(status, firstNotification.getStatus());
	}
}
