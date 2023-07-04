/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

final class MockedExecutorWrapper {
    private final List<Runnable> submittedTasksToExecutor;

    MockedExecutorWrapper() {
        submittedTasksToExecutor = new ArrayList<>();
    }

    Executor getMockedExecutor() {
        return runnable -> {
            submittedTasksToExecutor.add(runnable);
            runnable.run();
        };
    }

    void assertSubmittedTasksSize(final int taskCount) {
        assertEquals(taskCount, submittedTasksToExecutor.size());
    }
}
