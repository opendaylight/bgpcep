/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.counters;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BigIntegerCounter {
    private static final Logger LOG = LoggerFactory.getLogger(BigIntegerCounter.class);

    private static final BigInteger MAX_VALUE = new BigInteger("18446744073709551615");
    private final LongAdder counter = new LongAdder();
    private final String counterName;

    public BigIntegerCounter(@Nonnull final String counterName) {
        this.counterName = Preconditions.checkNotNull(counterName);
    }

    public BigInteger incrementCount(final BigInteger change) {
        final BigInteger result;
        if (change.equals(BigInteger.ZERO)) {
            result = getCount();
        } else {
            Preconditions.checkArgument(change.compareTo(BigInteger.ZERO) > 0, "Count number %s must be a positive number.", change);
            this.counter.add(change.longValue());
            result = getCount();
            LOG.debug("Counter [{}] is incremented by {}. Current count: {}", this.counterName, change, result);
            if (change.compareTo(MAX_VALUE) > 0) {
                LOG.warn("Counter [{}] has exceeded the max allowed value {}. Counter's current value {} is invalid.", this.counterName, MAX_VALUE, result);
            }
        }
        return result;
    }

    public BigInteger incrementCount() {
        this.counter.increment();
        LOG.debug("Counter [{}] is incremented.", this.counterName);
        final BigInteger result = getCount();
        if (result.compareTo(MAX_VALUE) > 0) {
            LOG.warn("Counter [{}] has exceeded the max allowed value {}. Counter's current value {} is invalid.",
                this.counterName, MAX_VALUE, result);
        }
        return getCount();
    }

    public BigInteger decrementCount() {
        this.counter.decrement();
        LOG.debug("Counter [{}] is decremented.", this.counterName);
        final BigInteger result = getCount();
        if (result.compareTo(BigInteger.ZERO) < 0) {
            LOG.warn("Counter [{}] has exceeded the min allowed value {}. Counter's current value {} is invalid.",
                this.counterName, MAX_VALUE, result);
        }
        return result;
    }

    public BigInteger decrementCount(final BigInteger change) {
        final BigInteger result;
        if (change.equals(BigInteger.ZERO)) {
            result = getCount();
        } else {
            Preconditions.checkArgument(change.compareTo(BigInteger.ZERO) > 0, "Count number %s must be a positive number.", change);
            for (int i = 0; i < change.longValue(); i++) {
                this.counter.decrement();
            }
            result = getCount();
            LOG.debug("Counter [{}] is decremented by {}. Current count: {}", this.counterName, change, result);
            if (change.compareTo(BigInteger.ZERO) < 0) {
                // In most case, we do not want the BGP session get into trouble due to an ERROR in counter
                // so here we print ERROR log instead of throwing out exception
                LOG.warn("Counter {} must not be less than 0. Counter's current value {} is invalid.", this.counterName, result);
            }
        }
        return result;
    }

    public BigInteger getCount() {
        return UnsignedLong.valueOf(this.counter.longValue()).bigIntegerValue();
    }

    public void resetCount() {
        LOG.debug("Value of counter [{}] is reset to 0.", this.counterName);
        this.counter.reset();
    }
}
