/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.data.change.counter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.data.change.counter.rev140815.DataChangeCounter;

public class LinkstateTopologyDataChangeCounterTest extends AbstractDataBrokerTest {

    @Test
    public void testDataChangeCounter() throws InterruptedException, ExecutionException {
        final LinkstateTopologyDataChangeCounter counter = new LinkstateTopologyDataChangeCounter(getDataBroker());
        final Optional<Long> count = getCount();
        assertTrue(count.isPresent());
        assertEquals(0, count.get().longValue());

        counter.onDataChanged(null);
        final Optional<Long> countAfterDataChange = getCount();
        assertTrue(countAfterDataChange.isPresent());
        assertEquals(1, countAfterDataChange.get().longValue());

        counter.close();
        final Optional<Long> countAfterClose = getCount();
        assertFalse(countAfterClose.isPresent());
    }

    private Optional<Long> getCount() throws InterruptedException, ExecutionException {
        final ReadTransaction rTx = getDataBroker().newReadOnlyTransaction();
        final Optional<DataChangeCounter> dataMaybe = rTx.read(LogicalDatastoreType.OPERATIONAL, LinkstateTopologyDataChangeCounter.IID).get();
        if (dataMaybe.isPresent()) {
            return Optional.of(dataMaybe.get().getCount());
        }
        return Optional.absent();
    }
}
