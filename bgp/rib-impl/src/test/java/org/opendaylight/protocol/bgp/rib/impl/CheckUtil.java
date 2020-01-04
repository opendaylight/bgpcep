/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.Assert;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;

public final class CheckUtil {
    private static final int SLEEP_FOR_MILLIS = 50;
    private static final int TIMEOUT_SECONDS = 10;

    private CheckUtil() {
        // Hidden on purpose
    }

    public static void checkIdleState(final SimpleSessionListener sessionListener) {
        checkInLoop(State.IDLE, sessionListener, SimpleSessionListener::getState, SLEEP_FOR_MILLIS, TIMEOUT_SECONDS);
    }

    public static void checkIdleState(final BGPPeer bgpPeer) {
        checkInLoop(State.IDLE, bgpPeer, peer -> {
            synchronized (bgpPeer) {
                final BGPSessionState state = peer.getBGPSessionState();
                return state == null ? State.IDLE : state.getSessionState();
            }
        }, SLEEP_FOR_MILLIS, TIMEOUT_SECONDS);
    }

    public static void checkUpState(final SimpleSessionListener sessionListener) {
        checkInLoop(State.UP, sessionListener, SimpleSessionListener::getState, SLEEP_FOR_MILLIS, TIMEOUT_SECONDS);
    }

    public static void checkUpState(final BGPPeer bgpPeer) {
        checkInLoop(State.UP, bgpPeer, peer -> {
            synchronized (bgpPeer) {
                final BGPSessionState state = peer.getBGPSessionState();
                return state == null ? State.IDLE : state.getSessionState();
            }
        }, SLEEP_FOR_MILLIS, TIMEOUT_SECONDS);
    }

    private static <T> void checkInLoop(final State state, final T object, final Function<T, State> function,
                                    final int sleepForMillis, final int timeoutSeconds) {
        final long timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.NANOSECONDS) <= timeoutNanos) {
            if (state != function.apply(object)) {
                Uninterruptibles.sleepUninterruptibly(sleepForMillis, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }

    public static void checkStateIsNotRestarting(final BGPPeer peer, final int restartTimeSeconds) {
        final long restartTimeNanos = TimeUnit.SECONDS.toNanos(restartTimeSeconds + 1);
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.NANOSECONDS) <= restartTimeNanos) {
            final BGPGracelfulRestartState restartState = peer.getPeerState().getBGPGracelfulRestart();
            if (restartState.isPeerRestarting() || restartState.isLocalRestarting()) {
                Uninterruptibles.sleepUninterruptibly(SLEEP_FOR_MILLIS, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }
}
