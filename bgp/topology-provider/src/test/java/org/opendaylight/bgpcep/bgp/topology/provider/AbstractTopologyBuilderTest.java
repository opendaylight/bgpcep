/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTopologyBuilderTest extends AbstractConcurrentDataBrokerTest {
    static final TopologyId TEST_TOPOLOGY_ID = new TopologyId("test-topo");
    static final RibReference LOC_RIB_REF = new DefaultRibReference(InstanceIdentifier.create(BgpRib.class)
        .child(Rib.class, new RibKey(requireNonNull(new RibId("test-rib")))));

    @Before
    public void setUp() {
        createEmptyTopology();
    }

    private void createEmptyTopology() {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(NetworkTopology.class).build(),
            new NetworkTopologyBuilder().setTopology(Collections.emptyList()).build());
        wTx.submit();
    }
}
