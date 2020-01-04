/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.util;

import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;

/**
 * Statistics utility class.
 */
public final class StatisticsUtil {
    private StatisticsUtil() {
        // Hidden on purpose
    }

    /**
     * Formats elapsed time in seconds to form days:hours:minutes:seconds.
     *
     * @param seconds Elapsed time in seconds.
     * @return Formated time as string d:hh:mm:ss
     */
    public static String formatElapsedTime(final long seconds) {
        Preconditions.checkArgument(seconds >= 0);
        return String.format("%1d:%02d:%02d:%02d",
                TimeUnit.SECONDS.toDays(seconds),
                TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(TimeUnit.SECONDS.toDays(seconds)),
                TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds)),
                seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
    }

    /**
     * Get current time in seconds. See also {@link System#currentTimeMillis()}.
     *
     * @return the difference, measured in seconds, between the current time and midnight, January 1, 1970 UTC.
     */
    public static long getCurrentTimestampInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
