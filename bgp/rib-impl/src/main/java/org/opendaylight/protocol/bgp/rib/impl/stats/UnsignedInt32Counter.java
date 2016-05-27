/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
@ThreadSafe
public class UnsignedInt32Counter {
    private static final Logger LOG = LoggerFactory.getLogger(UnsignedInt32Counter.class);
    private static final long MAX_VALUE = 4294967295L;

    private final String counterName;
    private final AtomicLong count = new AtomicLong();

    public UnsignedInt32Counter(@Nonnull final String counterName) {
        this.counterName = Preconditions.checkNotNull(counterName);
    }

    private static long safetyCheck(final long value) {
        return Math.min(Math.max(0L, value), MAX_VALUE);
    }

    public long increaseCount(final long change) {
        final long result;
        if (change == 0) {
            result = getCount();
        } else {
            Preconditions.checkArgument(change > 0, "Count number %s must be a positive number.", change);
            result = this.count.addAndGet(change);
            LOG.debug("Counter [{}] is increased by {}. Current count: {}", this.counterName, change, result);
            if (result > MAX_VALUE) {
                LOG.warn("Counter [{}] has exceeded the max allowed value {}. Counter's current value {} is invalid.", this.counterName, MAX_VALUE, result);
            }
        }
        return result;
    }

    public long increaseCount() {
        return this.increaseCount(1);
    }

    public long decreaseCount(final long change) {
        final long result;
        if (change == 0) {
            result = getCount();
        } else {
            Preconditions.checkArgument(change > 0, "Count number %s must be a positive number.", change);
            result = this.count.addAndGet(-change);
            LOG.debug("Counter [{}] is decreased by {}. Current count: {}", this.counterName, change, result);
            if (result < 0) {
                // In most case, we do not want the BGP session get into trouble due to an ERROR in counter
                // so here we print ERROR log instead of throwing out exception
                LOG.warn("Counter {} must not be less than 0. Counter's current value {} is invalid.", this.counterName, result);
            }
        }
        return result;
    }

    public long decreaseCount() {
        return this.decreaseCount(1);
    }

    public long getCount() {
        return this.count.get();
    }

    public void setCount(final long count) {
        final long result = getCount();
        if (count != result) {
            LOG.debug("Value of counter [{}] changes to {} (previous value is {})", this.counterName, count, result);
            this.count.set(count);
        }
    }

    public void resetCount() {
        LOG.debug("Value of counter [{}] is reset to 0.", this.counterName);
        setCount(0L);
    }

    public ZeroBasedCounter32 getCountAsZeroBasedCounter32() {
        return new ZeroBasedCounter32(safetyCheck(getCount()));
    }
}
