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
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BigIntegerCounter {
    private static final Logger LOG = LoggerFactory.getLogger(BigIntegerCounter.class);
    private static final BigInteger MAX_VALUE = new BigInteger("18446744073709551615");

    private final String counterName;
    private final AtomicReference<BigInteger> count = new AtomicReference<>(BigInteger.ZERO);

    public BigIntegerCounter(@Nonnull final String counterName) {
        this.counterName = Preconditions.checkNotNull(counterName);
    }

    public BigInteger increaseCount(final BigInteger change) {
        final BigInteger result;
        if (change.equals(BigInteger.ZERO)) {
            result = getCount();
        } else {
            Preconditions.checkArgument(change.compareTo(BigInteger.ZERO) > 0, "Count number %s must be a positive number.", change);
            result = this.count.accumulateAndGet(change, BigInteger::add);
            LOG.debug("Counter [{}] is increased by {}. Current count: {}", this.counterName, change, result);
            if (change.compareTo(MAX_VALUE) > 0) {
                LOG.warn("Counter [{}] has exceeded the max allowed value {}. Counter's current value {} is invalid.", this.counterName, MAX_VALUE, result);
            }
        }
        return result;
    }

    public BigInteger increaseCount() {
        return this.increaseCount(BigInteger.ONE);
    }

    public BigInteger decreaseCount(final BigInteger change) {
        final BigInteger result;
        if (change.equals(BigInteger.ZERO)) {
            result = getCount();
        } else {
            Preconditions.checkArgument(change.compareTo(BigInteger.ZERO) > 0, "Count number %s must be a positive number.", change);
            result = this.count.accumulateAndGet(change, BigInteger::subtract);
            LOG.debug("Counter [{}] is decreased by {}. Current count: {}", this.counterName, change, result);
            if (change.compareTo(BigInteger.ZERO) < 0) {
                // In most case, we do not want the BGP session get into trouble due to an ERROR in counter
                // so here we print ERROR log instead of throwing out exception
                LOG.warn("Counter {} must not be less than 0. Counter's current value {} is invalid.", this.counterName, result);
            }
        }
        return result;
    }

    public BigInteger decreaseCount() {
        return this.decreaseCount(BigInteger.ONE);
    }

    public BigInteger getCount() {
        return this.count.get();
    }

    public void setCount(final BigInteger count) {
        final BigInteger result = getCount();
        if (!count.equals(result)) {
            LOG.debug("Value of counter [{}] changes to {} (previous value is {})", this.counterName, count, result);
            this.count.set(count);
        }
    }

    public void resetCount() {
        LOG.debug("Value of counter [{}] is reset to 0.", this.counterName);
        setCount(BigInteger.ZERO);
    }
}
