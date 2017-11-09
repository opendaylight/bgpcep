/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatusChanged;
import org.opendaylight.yangtools.yang.binding.Notification;

final class MockedNotificationServiceWrapper {
    private final List<Notification> publishedNotifications;

    MockedNotificationServiceWrapper() {
        this.publishedNotifications = Lists.newArrayList();
    }

    NotificationPublishService getMockedNotificationService() throws InterruptedException {
        final NotificationPublishService mockedNotificationService = mock(NotificationPublishService.class);

        doAnswer(invocation -> {
            final Object notif = invocation.getArguments()[0];
            assertTrue(Notification.class.isAssignableFrom(notif.getClass()));
            MockedNotificationServiceWrapper.this.publishedNotifications.add((Notification) notif);
            return null;
        }).when(mockedNotificationService).putNotification(any(Notification.class));
        return mockedNotificationService;
    }

    void assertNotificationsCount(final int count) throws Exception {
        checkEquals(() -> assertEquals(count, this.publishedNotifications.size()));
    }

    void assertInstructionStatusChangedNotification(final int idx, final InstructionId id,
            final InstructionStatus status) {
        assertTrue(InstructionStatusChanged.class.isAssignableFrom(this.publishedNotifications.get(idx).getClass()));
        final InstructionStatusChanged firstNotification =
                (InstructionStatusChanged) this.publishedNotifications.get(idx);
        assertInstructionStatusChangedNotification(id, status, firstNotification);
    }

    private static void assertInstructionStatusChangedNotification(final InstructionId id,
            final InstructionStatus status, final InstructionStatusChanged firstNotification) {
        Assert.assertEquals(id, firstNotification.getId());
        Assert.assertEquals(status, firstNotification.getStatus());
    }
}
