/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.counters;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;

public class CounterTest {
    private static final long SECOND_INCREMENT_LONG = 5;
    private static final BigInteger SECOND_INCREMENT_BIG_INTEGER = new BigInteger("5");

    @Test
    public void unsignedInt32Test() throws Exception {
        final UnsignedInt32Counter counter = new UnsignedInt32Counter("test-counter");
        assertEquals(1, counter.increaseCount());
        assertEquals(6, counter.increaseCount(SECOND_INCREMENT_LONG));
        assertEquals(SECOND_INCREMENT_LONG, counter.decreaseCount());
        assertEquals(0, counter.decreaseCount(SECOND_INCREMENT_LONG));
        counter.setCount(SECOND_INCREMENT_LONG);
        assertEquals(SECOND_INCREMENT_LONG, counter.getCount());
        assertEquals(new ZeroBasedCounter32(SECOND_INCREMENT_LONG), counter.getCountAsZeroBasedCounter32());
        counter.resetCount();
        assertEquals(0, counter.getCount());
    }

    @Test
    public void bigIntegerCounterTest() throws Exception {
        final BigIntegerCounter counter = new BigIntegerCounter("test-counter");
        assertEquals(BigInteger.ONE, counter.increaseCount());
        assertEquals(new BigInteger("6"), counter.increaseCount(SECOND_INCREMENT_BIG_INTEGER));
        assertEquals(SECOND_INCREMENT_BIG_INTEGER, counter.decreaseCount());
        assertEquals(BigInteger.ZERO, counter.decreaseCount(SECOND_INCREMENT_BIG_INTEGER));
        counter.setCount(SECOND_INCREMENT_BIG_INTEGER);
        assertEquals(SECOND_INCREMENT_BIG_INTEGER, counter.getCount());
        counter.resetCount();
        assertEquals(BigInteger.ZERO, counter.getCount());
    }

    @Test
    public void bgpCountersMessagesTypesCommonTest() throws Exception {
        final BGPCountersMessagesTypesCommon counter = new BGPCountersMessagesTypesCommon("127.0.0.1", "sent");
        counter.increaseNotification();
        assertEquals(BigInteger.ONE, counter.getNotificationCount());
        counter.increaseUpdate();
        assertEquals(BigInteger.ONE, counter.getUpdateCount());
    }
}