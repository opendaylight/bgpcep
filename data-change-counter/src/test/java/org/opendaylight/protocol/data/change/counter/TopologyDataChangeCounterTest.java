/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.data.change.counter;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.util.CheckUtil.checkNull;
import static org.opendaylight.protocol.util.CheckUtil.checkPresent;
import static org.opendaylight.protocol.util.CheckUtil.readData;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.DataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev160315.data.change.counter.CounterKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TopologyDataChangeCounterTest extends AbstractDataBrokerTest {

    private static final String COUNTER_ID1 = "counter1";
    private static final String COUNTER_ID2 = "counter2";
    private final InstanceIdentifier<Counter> counterInstanceId_1 = InstanceIdentifier.builder(DataChangeCounter.class)
        .child(Counter.class, new CounterKey(COUNTER_ID1)).build();
    private final InstanceIdentifier<Counter> counterInstanceId_2 = InstanceIdentifier.builder(DataChangeCounter.class)
        .child(Counter.class, new CounterKey(COUNTER_ID2)).build();

    @Test
    public void testDataChangeCounter() throws Exception {
        final TopologyDataChangeCounter counter = new TopologyDataChangeCounter(getDataBroker(), COUNTER_ID1);
        readData(getDataBroker(), this.counterInstanceId_1, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        counter.onDataTreeChanged(null);
        readData(getDataBroker(), this.counterInstanceId_1, count -> {
            assertEquals(1, count.getCount().longValue());
            return count;
        });

        counter.close();
        checkNull(getDataBroker(), this.counterInstanceId_1);
    }

    @Test
    public void testDataChangeCounterTwoInstances() throws Exception {
        final TopologyDataChangeCounter counter1 = new TopologyDataChangeCounter(getDataBroker(), COUNTER_ID1);
        readData(getDataBroker(), this.counterInstanceId_1, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        final TopologyDataChangeCounter counter2 = new TopologyDataChangeCounter(getDataBroker(), COUNTER_ID2);
        readData(getDataBroker(), this.counterInstanceId_2, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        counter1.onDataTreeChanged(null);
        readData(getDataBroker(), this.counterInstanceId_1, count -> {
            assertEquals(1, count.getCount().longValue());
            return count;
        });
        readData(getDataBroker(), this.counterInstanceId_2, count -> {
            assertEquals(0, count.getCount().longValue());
            return count;
        });

        counter1.close();
        checkNull(getDataBroker(), this.counterInstanceId_1);
        // Check that counter2 does not get deleted
        checkPresent(getDataBroker(), this.counterInstanceId_2);

        counter2.close();
        checkNull(getDataBroker(), this.counterInstanceId_2);
    }
}
