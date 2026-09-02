/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;

public final class CheckUtil {
    private static final Duration SLEEP_FOR_MILLIS = Duration.ofMillis(50);
    private static final Duration TIMEOUT_SECONDS = Duration.ofSeconds(10);

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
            final Duration sleepFor, final Duration timeout) {
        await().atMost(timeout).pollInterval(sleepFor).untilAsserted(
            () -> Assertions.assertEquals(state, function.apply(object)));
    }

    public static void checkStateIsNotRestarting(final BGPPeer peer, final int restartTimeSeconds) {
        await().atMost(Duration.ofSeconds(restartTimeSeconds + 1)).pollInterval(SLEEP_FOR_MILLIS).untilAsserted(() -> {
                final BGPGracelfulRestartState restartState = peer.getPeerState().getBGPGracelfulRestart();
                Assertions.assertFalse(restartState.isPeerRestarting() || restartState.isLocalRestarting());
            });
    }
}
