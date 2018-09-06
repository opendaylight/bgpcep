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
    private CheckUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T> void checkState (final T object, final State state, final Function<T, State> function) {
        checkInLoop(state, object, function, 50, 10);
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
                Uninterruptibles.sleepUninterruptibly((restartTime * 1000) - sw.elapsed(TimeUnit.MILLISECONDS),
                        TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }
}