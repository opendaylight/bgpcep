/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ApplicationPeerTest extends AbstractDataBrokerTest {

    private final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private RIBImpl r;

    @Mock
    BGPDispatcher dispatcher;

    @Mock
    ReconnectStrategyFactory tcpStrategyFactory;

    DataBroker dps;

    ReadWriteTransaction trans;

    @Mock
    ApplicationPeer peer;

    @Mock
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;

    @Before
    public void setUp() throws InterruptedException, ExecutionException {
        MockitoAnnotations.initMocks(this);
        final List<BgpTableType> localTables = new ArrayList<>();
        this.dps = getDataBroker();
        localTables.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        localTables.add(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        final RIBActivator a1 = new RIBActivator();
        a1.startRIBExtensionProvider(context);
        final org.opendaylight.protocol.bgp.linkstate.RIBActivator a2 = new org.opendaylight.protocol.bgp.linkstate.RIBActivator();
        a2.startRIBExtensionProvider(context);
        this.trans = this.dps.newReadWriteTransaction();
        //this.r = new RIBImpl(new RibId("test"), new AsNumber(5L), new Ipv4Address("127.0.0.1"),
        //    context , this.dispatcher, this.tcpStrategyFactory, this.tcpStrategyFactory, this.dps, localTables);
        // this.peer = new ApplicationPeer(new ApplicationRibId("t"), new Ipv4Address("127.0.0.1"), this.r);
    }

    @Test
    public void testOnDataChanged() {
        final Map<InstanceIdentifier<?>, DataObject> created = new HashMap<>();
        //final AdjRIBsIn<Ipv4Prefix, Ipv4Route> a4 = this.r.getTable(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        //assertNotNull(a4);

        final InstanceIdentifier<?> iid = InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(new RibId("foo")))
            .child(LocRib.class).child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class))
            .child(Ipv4Routes.class).child(Ipv4Route.class, new Ipv4RouteKey(new Ipv4Prefix("127.0.0.1")));

        created.put(iid, new Ipv4RouteBuilder().setAttributes(new AttributesBuilder().build()).build());
        final Map<InstanceIdentifier<?>, DataObject> updated = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> original = new HashMap<>();
        final Set<InstanceIdentifier<?>> removed = new HashSet<>();
        Mockito.doReturn(created).when(this.change).getCreatedData();
        Mockito.doReturn(updated).when(this.change).getUpdatedData();
        Mockito.doReturn(original).when(this.change).getOriginalData();
        Mockito.doReturn(removed).when(this.change).getRemovedPaths();
        //this.peer.onDataChanged(this.change);
    }

    @Test
    public void testClose() {
        // final AdjRIBsIn<Ipv4Prefix, Ipv4Route> a4 = this.r.getTable(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        // assertNotNull(a4);
        //this.peer.close();
    }
}
