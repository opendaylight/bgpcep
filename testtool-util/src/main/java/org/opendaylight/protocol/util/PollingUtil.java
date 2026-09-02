/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.awaitility.Awaitility.dontCatchUncaughtExceptions;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

/**
 * Polling loop shared by {@link CheckUtil} and {@link CheckTestUtil}.
 */
final class PollingUtil {
    @FunctionalInterface
    interface Check {
        void run() throws ExecutionException, InterruptedException;
    }

    private PollingUtil() {
        // Hidden on purpose
    }

    /**
     * Re-runs {@code check} until it completes without throwing an {@link AssertionError}, failing with
     * {@code ConditionTimeoutException} once {@code timeout} elapses.
     *
     * @param timeout how long to keep polling
     * @param interval pause between attempts
     * @param action what {@code check} does, used in failure messages as "Failed to {@code action}"
     * @param check the check to run
     */
    static void pollUntilAsserted(final Duration timeout, final Duration interval, final String action,
            final Check check) {
        // Poll on the caller's thread, as the hand-rolled loops did
        dontCatchUncaughtExceptions().pollInSameThread().atMost(timeout).pollInterval(interval)
            .pollDelay(Duration.ZERO)
            .untilAsserted(() -> {
                try {
                    check.run();
                } catch (ExecutionException e) {
                    // untilAsserted() retries AssertionError only, so surface a failure as one
                    throw new AssertionError("Failed to " + action, e);
                } catch (InterruptedException e) {
                    // This is the caller's thread, so keep its interrupt visible and abort the poll
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while trying to " + action, e);
                }
            });
    }
}
