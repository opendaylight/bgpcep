/**
 * @author Maros Marsalek
 *
 * 01 2014
 *
 * Copyright (c) 2012 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatusChanged;
import org.opendaylight.yangtools.yang.binding.Notification;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

final class MockedNotificationServiceWrapper {
    private List<Notification> publishedNotifications;

    MockedNotificationServiceWrapper() {
        publishedNotifications = Lists.newArrayList();
    }

    NotificationProviderService getMockedNotificationService() {
        NotificationProviderService mockedNotificationService = mock(NotificationProviderService.class);

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

    void assertNotificationsCount(int count) {
        assertEquals(count, publishedNotifications.size());
    }

    public void assertInstructionStatusChangedNotification(int idx, InstructionId id, InstructionStatus status) {
        assertTrue(InstructionStatusChanged.class.isAssignableFrom(publishedNotifications.get(idx).getClass()));
        InstructionStatusChanged firstNotification = (InstructionStatusChanged) publishedNotifications.get(idx);
        assertInstructionStatusChangedNotification(id, status, firstNotification);
    }

    private void assertInstructionStatusChangedNotification(InstructionId id, InstructionStatus status,
            InstructionStatusChanged firstNotification) {
        Assert.assertEquals(id, firstNotification.getId());
        Assert.assertEquals(status, firstNotification.getStatus());
    }
}
