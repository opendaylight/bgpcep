/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.opendaylight.protocol.util.CheckTestUtil.checkEquals;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatusChanged;
import org.opendaylight.yangtools.yang.binding.Notification;

final class MockedNotificationServiceWrapper {
    private final List<Notification<?>> publishedNotifications = new ArrayList<>();

    NotificationPublishService getMockedNotificationService() throws InterruptedException {
        final NotificationPublishService mockedNotificationService = mock(NotificationPublishService.class);

        doAnswer(invocation -> {
            final Object notif = invocation.getArguments()[0];
            assertThat(notif, instanceOf(Notification.class));
            MockedNotificationServiceWrapper.this.publishedNotifications.add((Notification<?>) notif);
            return null;
        }).when(mockedNotificationService).putNotification(any(Notification.class));
        return mockedNotificationService;
    }

    void assertNotificationsCount(final int count) throws Exception {
        checkEquals(() -> assertEquals(count, publishedNotifications.size()));
    }

    void assertInstructionStatusChangedNotification(final int idx, final InstructionId id,
            final InstructionStatus status) {
        assertTrue(InstructionStatusChanged.class.isAssignableFrom(publishedNotifications.get(idx).getClass()));
        final InstructionStatusChanged firstNotification =
                (InstructionStatusChanged) publishedNotifications.get(idx);
        assertInstructionStatusChangedNotification(id, status, firstNotification);
    }

    private static void assertInstructionStatusChangedNotification(final InstructionId id,
            final InstructionStatus status, final InstructionStatusChanged firstNotification) {
        assertEquals(id, firstNotification.getId());
        assertEquals(status, firstNotification.getStatus());
    }
}
