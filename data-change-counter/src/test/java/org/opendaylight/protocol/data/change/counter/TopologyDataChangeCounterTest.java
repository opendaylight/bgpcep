/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.data.change.counter;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.util.CheckTestUtil.checkNotPresentOperational;
import static org.opendaylight.protocol.util.CheckTestUtil.checkPresentOperational;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataOperational;

import org.junit.Test;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TopologyDataChangeCounterTest extends AbstractConcurrentDataBrokerTest {

    private static final String COUNTER_ID1 = "counter1";
    private static final String COUNTER_ID2 = "counter2";
    private static final String TOPOLOGY_NAME = "example-linkstate-topology";
    private final InstanceIdentifier<Counter> counterIId1 = InstanceIdentifier.builder(DataChangeCounter.class)
            .child(Counter.class, new CounterKey(COUNTER_ID1)).build();
    private final InstanceIdentifier<Counter> counterIId2 = InstanceIdentifier.builder(DataChangeCounter.class)
            .child(Counter.class, new CounterKey(COUNTER_ID2)).build();

    @Test
    public void testDataChangeCounter() throws Exception {
        final TopologyDataChangeCounter counter = new TopologyDataChangeCounter(getDataBroker(),
                COUNTER_ID1, TOPOLOGY_NAME);
        readDataOperational(getDataBroker(), this.counterIId1, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        counter.onDataTreeChanged(null);
        readDataOperational(getDataBroker(), this.counterIId1, count -> {
            assertEquals(1, count.getCount().longValue());
            return count;
        });

        counter.close();
        checkNotPresentOperational(getDataBroker(), this.counterIId1);
    }

    @Test
    public void testDataChangeCounterTwoInstances() throws Exception {
        final TopologyDataChangeCounter counter1 = new TopologyDataChangeCounter(getDataBroker(),
                COUNTER_ID1, TOPOLOGY_NAME);
        readDataOperational(getDataBroker(), this.counterIId1, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        final TopologyDataChangeCounter counter2 = new TopologyDataChangeCounter(getDataBroker(),
                COUNTER_ID2, TOPOLOGY_NAME);
        readDataOperational(getDataBroker(), this.counterIId2, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        counter1.onDataTreeChanged(null);
        readDataOperational(getDataBroker(), this.counterIId1, count -> {
            assertEquals(1, count.getCount().longValue());
            return count;
        });
        readDataOperational(getDataBroker(), this.counterIId2, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        counter1.close();
        checkNotPresentOperational(getDataBroker(), this.counterIId1);
        // Check that counter2 does not get deleted
        checkPresentOperational(getDataBroker(), this.counterIId2);

        counter2.close();
        checkNotPresentOperational(getDataBroker(), this.counterIId2);
    }
}
