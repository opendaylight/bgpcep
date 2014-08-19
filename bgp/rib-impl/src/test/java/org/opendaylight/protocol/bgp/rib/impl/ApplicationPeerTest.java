/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ApplicationPeerTest {

    private final List<Update> updates = new ArrayList<>();

    private RIBImpl r;

    @Mock
    BGPDispatcher dispatcher;

    @Mock
    ReconnectStrategyFactory tcpStrategyFactory;

    @Mock
    DataBroker dps;

    @Mock
    ReadWriteTransaction trans;

    @Mock
    CheckedFuture<?, ?> rib;

    @Mock
    Optional<Rib> o;

    @Mock
    ListenableFuture<Void> future;

    @Mock
    ApplicationPeer peer;

    @Mock
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;

    @Mock
    InstanceIdentifier<?> id;

    @Before
    public void setUp() throws InterruptedException, ExecutionException {
        MockitoAnnotations.initMocks(this);
        final List<BgpTableType> localTables = new ArrayList<>();
        localTables.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        localTables.add(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        final RIBActivator a1 = new RIBActivator();
        a1.startRIBExtensionProvider(context);
        final org.opendaylight.protocol.bgp.linkstate.RIBActivator a2 = new org.opendaylight.protocol.bgp.linkstate.RIBActivator();
        a2.startRIBExtensionProvider(context);
        Mockito.doReturn(this.trans).when(this.dps).newReadWriteTransaction();
        Mockito.doReturn(this.trans).when(this.dps).newWriteOnlyTransaction();
        Mockito.doReturn(this.rib).when(this.trans).read(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class));
        Mockito.doNothing().when(this.trans).merge(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(BgpRib.class));
        Mockito.doNothing().when(this.trans).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(BgpRib.class));
        Mockito.doReturn(this.rib).when(this.trans).submit();
        Mockito.doNothing().when(this.rib).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doReturn(this.o).when(this.rib).get();
        Mockito.doReturn(Boolean.FALSE).when(this.o).isPresent();
        this.r = new RIBImpl(new RibId("test"), new AsNumber(5L), new Ipv4Address("127.0.0.1"),
            context , this.dispatcher, this.tcpStrategyFactory, this.tcpStrategyFactory, this.dps, localTables);
        this.peer = new ApplicationPeer(new ApplicationRibId("t"), new Ipv4Address("127.0.0.1"), this.r);
    }

    @Test
    public void testOnDataChanged() {
        final Map<InstanceIdentifier<?>, DataObject> created = new HashMap<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class);
        final List<Ipv4Prefix> v4prefs = Lists.newArrayList( new Ipv4Prefix("127.0.0.1"));
        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(v4prefs).build()).build()).build());
        created.put(this.id, new Ipv4RouteBuilder().setAttributes(new AttributesBuilder().setMpReachNlri(mp.build()).build()).build());
        final Map<InstanceIdentifier<?>, DataObject> updated = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> original = new HashMap<>();
        final Set<InstanceIdentifier<?>> removed = new HashSet<>();
        Mockito.doReturn(created).when(this.change).getCreatedData();
        Mockito.doReturn(updated).when(this.change).getUpdatedData();
        Mockito.doReturn(original).when(this.change).getOriginalData();
        Mockito.doReturn(removed).when(this.change).getRemovedPaths();
        this.peer.onDataChanged(this.change);
        final AdjRIBsIn<Ipv4Prefix, Ipv4Route> a4 = this.r.getTable(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        Mockito.verify(this.trans).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(InstanceIdentifier.class), Mockito.any(BgpRib.class));
    }

    @Test
    public void testClose() {
        final AdjRIBsIn<Ipv4Prefix, Ipv4Route> a4 = this.r.getTable(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        assertNotNull(a4);
        this.peer.close();
    }
}
