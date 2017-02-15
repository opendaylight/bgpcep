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
import org.junit.Assert;
import org.opendaylight.protocol.bgp.rib.spi.State;

public final class CheckUtil {
    public static void checkIdleState(final SimpleSessionListener listener) {
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            if (State.IDLE != listener.getState()) {
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }
}