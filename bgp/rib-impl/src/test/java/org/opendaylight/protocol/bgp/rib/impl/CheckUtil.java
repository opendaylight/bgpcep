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

public final class CheckUtil {

    private static final int SLEEP_FOR = 50;
    private static final int TIMEOUT = 10;

    private CheckUtil() {
        throw new UnsupportedOperationException();
    }

    public static void checkIdleState(final SimpleSessionListener sessionListener) {
        checkInLoop(State.IDLE, sessionListener, listener -> listener.getState(), SLEEP_FOR, TIMEOUT);
    }

    public static void checkIdleState(final BGPPeer bgpPeer) {
        checkInLoop(State.IDLE, bgpPeer, peer -> {
            if (peer.getBGPSessionState() == null) {
                return State.IDLE;
            } else {
                return peer.getBGPSessionState().getSessionState();
            }
        }, SLEEP_FOR, TIMEOUT);
    }

    public static void checkUpState(final SimpleSessionListener sessionListener) {
        checkInLoop(State.UP, sessionListener, listener -> listener.getState(), SLEEP_FOR, TIMEOUT);
    }

    public static void checkUpState(final BGPPeer bgpPeer) {
        checkInLoop(State.UP, bgpPeer, peer -> {
                    if (peer.getBGPSessionState() == null) {
                        return State.IDLE;
                    } else {
                        return peer.getBGPSessionState().getSessionState();
                    }
                }, SLEEP_FOR, TIMEOUT);
    }

    private static <T> void checkInLoop(final State state, final T object, final Function<T, State> function,
                                    final int sleepFor, final int timeout) {
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= timeout) {
            if (state != function.apply(object)) {
                Uninterruptibles.sleepUninterruptibly(sleepFor, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }

    public static void checkRestartState(final BGPPeer peer, final int restartTime) {
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= restartTime + 1) {
            if (peer.getPeerState().getBGPGracelfulRestart().isPeerRestarting()) {
                Uninterruptibles.sleepUninterruptibly((TimeUnit.SECONDS.toMillis(restartTime))
                                - sw.elapsed(TimeUnit.MILLISECONDS),
                        TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }
}