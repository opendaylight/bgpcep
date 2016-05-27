/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public class UnsignedInt32Counter {
    private static final Logger LOG = LoggerFactory.getLogger(UnsignedInt32Counter.class);

    private final String counterName;

    @GuardedBy("this")
    private long count = 0L;

    public UnsignedInt32Counter(@Nonnull final String counterName) {
        this.counterName = Preconditions.checkNotNull(counterName);
    }

    public long increaseCount(final long count) {
        this.count += count;
        LOG.debug("Counter [{}] is increased by {}. Current count: {}", this.counterName, count, this.count);
        return this.count;
    }

    public long increaseCount() {
        return this.increaseCount(1);
    }

    public long decreaseCount() {
        this.count--;
        LOG.debug("Counter [{}] is decreased by one. Current count: {}", this.counterName, this.count);
        Preconditions.checkState(this.count >= 0, "Counter {} must never be less than 0.", this.counterName);
        return this.count;
    }

    public long getCount() {
        return this.count;
    }

    public void setCount(final long count) {
        if (count != this.count) {
            LOG.debug("Value of counter [{}] changes to {} (previous value is {})", this.counterName, count, this.count);
        }
        this.count = count;
    }

    public void resetCount() {
        LOG.debug("Value of counter [{}] is reset to 0.", this.counterName);
        setCount(0L);
    }

    public ZeroBasedCounter32 getCountAsZeroBasedCounter32() {
        return new ZeroBasedCounter32(count);
    }
}
