/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.data.change.counter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TopologyDataChangeCounterTest extends AbstractConcurrentDataBrokerTest {

    private static final String COUNTER_ID1 = "counter1";
    private static final String COUNTER_ID2 = "counter2";

    @Test
    public void testDataChangeCounter() throws Exception {
        final TopologyDataChangeCounter counter = new TopologyDataChangeCounter(getDataBroker(), COUNTER_ID1);
        checkEquals(() -> {
            final Optional<Long> count = getCount(COUNTER_ID1);
            assertTrue(count.isPresent());
            assertEquals(0, count.get().longValue());
        });

        counter.onDataTreeChanged(null);
        checkEquals(() -> {
            final Optional<Long> countAfterDataChange = getCount(COUNTER_ID1);
            assertTrue(countAfterDataChange.isPresent());
            assertEquals(1, countAfterDataChange.get().longValue());
        });

        counter.close();
        checkEquals(() -> {
            final Optional<Long> countAfterClose = getCount(COUNTER_ID1);
            assertFalse(countAfterClose.isPresent());
        });

    }

    @Test
    public void testDataChangeCounterTwoInstances() throws Exception {
        final TopologyDataChangeCounter counter1 = new TopologyDataChangeCounter(getDataBroker(), COUNTER_ID1);
        checkEquals(() -> {
            final Optional<Long> count1 = getCount(COUNTER_ID1);
            assertTrue(count1.isPresent());
            assertEquals(0, count1.get().longValue());
        });

        final TopologyDataChangeCounter counter2 = new TopologyDataChangeCounter(getDataBroker(), COUNTER_ID2);
        checkEquals(() -> {
            final Optional<Long> count2 = getCount(COUNTER_ID2);
            assertTrue(count2.isPresent());
            assertEquals(0, count2.get().longValue());
        });

        counter1.onDataTreeChanged(null);
        checkEquals(() -> {
            final Optional<Long> countAfterDataChange1 = getCount(COUNTER_ID1);
            assertTrue(countAfterDataChange1.isPresent());
            assertEquals(1, countAfterDataChange1.get().longValue());
        });

        // Check that counter2 does not get incremented
        checkEquals(() -> {
            final Optional<Long> countAfterDataChange2 = getCount(COUNTER_ID2);
            assertTrue(countAfterDataChange2.isPresent());
            assertEquals(0, countAfterDataChange2.get().longValue());
        });

        counter1.close();
        checkEquals(() -> {
            final Optional<Long> countAfterClose1 = getCount(COUNTER_ID1);
            assertFalse(countAfterClose1.isPresent());
            // Check that counter2 does not get deleted
            final Optional<Long> countAfterClose2 = getCount(COUNTER_ID2);
            assertTrue(countAfterClose2.isPresent());
        });

        counter2.close();
        checkEquals(() -> {
            final Optional<Long> countAfterClose3 = getCount(COUNTER_ID2);
            assertFalse(countAfterClose3.isPresent());
        });

    }

    private Optional<Long> getCount(final String counterId) throws InterruptedException, ExecutionException {
        final ReadTransaction rTx = getDataBroker().newReadOnlyTransaction();
        final InstanceIdentifier<Counter> counterInstanceId = InstanceIdentifier.builder(DataChangeCounter.class)
            .child(Counter.class, new CounterKey(counterId)).build();
        final Optional<Counter> dataMaybe = rTx.read(LogicalDatastoreType.OPERATIONAL, counterInstanceId).get();
        if (dataMaybe.isPresent()) {
            return Optional.of(dataMaybe.get().getCount());
        }
        return Optional.absent();
    }
}
