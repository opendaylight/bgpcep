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
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;

public final class CheckUtil {
    private static final int TIMEOUT = 40;
    private static final int SLEEP_UNINTERRUPTIBLY = 50;

    static void checkReceivedMessages(final SimpleSessionListener listener, final int numberOfMessages)
        throws ReadFailedException {
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= TIMEOUT) {
            if (listener.getListMsg().size() != numberOfMessages) {
                Uninterruptibles.sleepUninterruptibly(SLEEP_UNINTERRUPTIBLY, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }
}