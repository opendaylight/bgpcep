/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.counters;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import javax.annotation.Nonnull;

public final class BGPCountersMessagesTypesCommon {
    private final BigIntegerCounter update;
    private final BigIntegerCounter notification;

    public BGPCountersMessagesTypesCommon(@Nonnull final String neighbor, @Nonnull final String type) {
        Preconditions.checkNotNull(neighbor);
        Preconditions.checkNotNull(type);
        this.update = new BigIntegerCounter("Total Update Message " + type + " by neighbor " + neighbor);
        this.notification = new BigIntegerCounter("Total Notification Message " + type + " by neighbor " + neighbor);
    }

    public void increaseUpdate() {
        this.update.increaseCount();
    }

    public void increaseNotification() {
        this.notification.increaseCount();
    }

    public BigInteger getNotificationCount() {
        return this.notification.getCount();
    }

    public BigInteger getUpdateCount() {
        return this.update.getCount();
    }
}
