/*
 * Copyright (c) 2017 Inocybe Technologies, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import com.google.common.base.Preconditions;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyProvider;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1Builder;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractBgpTopologyProviderTest extends AbstractConcurrentDataBrokerTest {
    @Mock
    protected final BgpTopologyDeployer bgpTopologyDeployer = Mockito.mock(BgpTopologyDeployer.class);
    @Mock
    protected final AbstractRegistration serviceRegistration = Mockito.mock(AbstractRegistration.class);

    protected final TopologyTypes1Builder topologyTypes1Builder = new TopologyTypes1Builder();
    protected final TopologyId TEST_TOPOLOGY_ID = new TopologyId("test-topo");
    protected final RibReference LOC_RIB_REF = new DefaultRibReference(InstanceIdentifier.create(BgpRib.class)
            .child(Rib.class, new RibKey(Preconditions.checkNotNull(new RibId("test-rib")))));

    public void setUp() {
        Mockito.doReturn(getDataBroker()).when(bgpTopologyDeployer).getDataBroker();
        Mockito.doReturn(serviceRegistration).when(bgpTopologyDeployer).registerTopologyProvider(Mockito.any(BgpTopologyProvider.class));
        Mockito.doReturn(serviceRegistration).when(bgpTopologyDeployer).registerService(Mockito.any(TopologyReferenceSingletonServiceImpl.class));
        Mockito.doNothing().when(serviceRegistration).close();
    }
}
