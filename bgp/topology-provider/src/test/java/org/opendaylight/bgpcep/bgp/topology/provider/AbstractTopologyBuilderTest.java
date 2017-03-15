/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.fail;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTopologyBuilderTest extends AbstractDataBrokerTest {

    static final TopologyId TEST_TOPOLOGY_ID = new TopologyId("test-topo");
    static final RibReference LOC_RIB_REF = new DefaultRibReference(InstanceIdentifier.create(BgpRib.class)
        .child(Rib.class, new RibKey(Preconditions.checkNotNull(new RibId("test-rib")))));

    @Override
    protected void setupWithDataBroker(final DataBroker dataBroker) {
        super.setupWithDataBroker(dataBroker);
        createEmptyTopology();
    }

    protected void createEmptyTopology() {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(NetworkTopology.class).build(), new NetworkTopologyBuilder().setTopology(Collections.<Topology>emptyList()).build());
        wTx.submit();
    }

    protected Optional<Topology> getTopology(final InstanceIdentifier<Topology> topoIID) {
        final ReadTransaction rTx = getDataBroker().newReadOnlyTransaction();
        try {
            return rTx.read(LogicalDatastoreType.OPERATIONAL, topoIID).get();
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
        return Optional.absent();
    }

}
