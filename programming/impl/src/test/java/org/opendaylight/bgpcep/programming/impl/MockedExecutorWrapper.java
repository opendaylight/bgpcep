/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.List;
import java.util.concurrent.Callable;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

final class MockedExecutorWrapper {

    private final List<Object> submittedTasksToExecutor;

    MockedExecutorWrapper() {
        this.submittedTasksToExecutor = Lists.newArrayList();
    }

    ListeningExecutorService getMockedExecutor() {
        final ListeningExecutorService mockedExecutor = mock(ListeningExecutorService.class);
        final Answer<ListenableFuture<?>> submitAnswer = invocation -> {
            final Object task = invocation.getArguments()[0];
            this.submittedTasksToExecutor.add(task);

            Object result = null;
            if (task instanceof Runnable) {
                ((Runnable) task).run();
            } else if (task instanceof Callable) {
                result = ((Callable<?>) task).call();
            }

            final ListenableFuture<?> mockedFuture = mock(ListenableFuture.class);
            doReturn(result).when(mockedFuture).get();
            return mockedFuture;
        };
        doAnswer(submitAnswer).when(mockedExecutor).submit(any(Runnable.class));
        doAnswer(submitAnswer).when(mockedExecutor).submit(any(Callable.class));
        return mockedExecutor;
    }

    void assertSubmittedTasksSize(final int taskCount) {
        assertEquals(taskCount, this.submittedTasksToExecutor.size());
    }
}
