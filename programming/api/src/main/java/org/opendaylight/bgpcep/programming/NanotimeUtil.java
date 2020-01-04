/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming;

import java.util.concurrent.TimeUnit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.Nanotime;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util methods for {@link Nanotime}.
 */
public final class NanotimeUtil {
    private static final Logger LOG = LoggerFactory.getLogger(NanotimeUtil.class);

    private static volatile Long nanoTimeOffset = null;

    private NanotimeUtil() {
        // Hidden on purpose
    }

    /**
     * Returns current time in nanoseconds.
     *
     * @return Nanotime object filled with current time in nanoseconds.
     */
    public static Nanotime currentTime() {
        final long millis = Uint64.valueOf(System.currentTimeMillis()).longValue();
        return new Nanotime(Uint64.fromLongBits(TimeUnit.MILLISECONDS.toNanos(millis)));
    }

    /**
     * Returns calibrated current JVM nano time.
     *
     * @return Nanotime object filled with current JVM nano time.
     */
    public static Nanotime currentNanoTime() {
        if (nanoTimeOffset == null) {
            calibrate();
        }
        return new Nanotime(Uint64.fromLongBits(System.nanoTime() + nanoTimeOffset.longValue()));
    }

    /**
     * Calibrates the offset between the real-time clock providing System.currentTimeMillis() and the monotonic clock
     * providing System.nanoTime(). This method should be called whenever there is a hint of the two diverging: either
     * when time shifts or periodically.
     */
    private static void calibrate() {
        final long tm1 = System.currentTimeMillis();
        final long nt1 = System.nanoTime();
        final long tm2 = System.currentTimeMillis();
        final long nt2 = System.nanoTime();

        LOG.debug("Calibrated currentTime and nanoTime to {}m <= {}n <= {}m <= {}n", tm1, nt1, tm2, nt2);

        final long tm = (tm1 + tm2) / 2;
        final long nt = (nt1 + nt2) / 2;

        nanoTimeOffset = Long.valueOf(TimeUnit.MILLISECONDS.toNanos(tm) - nt);
    }
}
