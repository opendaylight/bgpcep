/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsTransaction;
import org.opendaylight.protocol.bgp.rib.spi.BGPObjectComparator;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RouteEncoder;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;


public class AdjacencyRIBsInTest extends AbstractDataBrokerTest {

    private WriteTransaction trans;

    private final DefaultRibReference rib = new DefaultRibReference(InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(new RibId("test"))).build());

    private final RIBActivator act = new RIBActivator();

    private final RIBExtensionProviderContext ctx = new SimpleRIBExtensionProviderContext();

    private AdjRIBsIn<?, ?> a1;

    private RIBTables tables;

    private final TablesKey ipv4key = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private final TablesKey ipv6key = new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private final TablesKey linkstateKey = new TablesKey(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);

    @Mock
    AdjRIBsTransaction adjTrans4;

    @Mock
    AdjRIBsTransaction adjTrans6;

    @Mock
    AdjRIBsTransaction adjTransLS;

    @Mock
    Peer peer;

    @Mock
    KeyedInstanceIdentifier<Tables, TablesKey> id;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.trans = getDataBroker().newWriteOnlyTransaction();
        this.act.startRIBExtensionProvider(this.ctx);
        this.tables = new RIBTables(this.ctx);
        Mockito.doReturn(new BGPObjectComparator(new AsNumber(72L))).when(this.adjTrans4).comparator();
        Mockito.doReturn(new BGPObjectComparator(new AsNumber(72L))).when(this.adjTrans6).comparator();
        Mockito.doReturn(new BGPObjectComparator(new AsNumber(72L))).when(this.adjTransLS).comparator();
        Mockito.doReturn("test").when(this.peer).toString();
        Mockito.doNothing().when(this.adjTrans4).advertise(Mockito.any(RouteEncoder.class), Mockito.anyObject(), Mockito.any(InstanceIdentifier.class), Mockito.eq(this.peer), Mockito.any(Route.class));
        Mockito.doNothing().when(this.adjTrans4).setUptodate(Mockito.any(InstanceIdentifier.class), Mockito.anyBoolean());
        Mockito.doNothing().when(this.adjTrans6).advertise(Mockito.any(RouteEncoder.class), Mockito.anyObject(), Mockito.any(InstanceIdentifier.class), Mockito.eq(this.peer), Mockito.any(Route.class));
        Mockito.doNothing().when(this.adjTrans6).setUptodate(Mockito.any(InstanceIdentifier.class), Mockito.anyBoolean());
        Mockito.doNothing().when(this.adjTransLS).advertise(Mockito.any(RouteEncoder.class), Mockito.anyObject(), Mockito.any(InstanceIdentifier.class), Mockito.eq(this.peer), Mockito.any(Route.class));
        Mockito.doNothing().when(this.adjTransLS).setUptodate(Mockito.any(InstanceIdentifier.class), Mockito.anyBoolean());
    }

    @Test
    public void testRIBTables() {
        this.a1 = this.tables.create(this.trans, this.rib, this.ipv4key);
        assertNotNull(this.a1);
        assertEquals(this.a1, this.tables.get(this.ipv4key));
    }

    @Test
    public void testAddRoutes() {
        this.a1 = this.tables.create(this.trans, this.rib, this.ipv4key);
        assertNotNull(this.a1);
        MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(
                Lists.newArrayList(new Ipv4PrefixesBuilder().setPrefix(new Ipv4Prefix("127.0.0.1/32")).build())).build()).build()).build());
        final PathAttributesBuilder paBuilder = new PathAttributesBuilder();
        this.a1.addRoutes(this.adjTrans4, this.peer, mpBuilder.build(), paBuilder.build());
        Mockito.verify(this.adjTrans4).advertise(Mockito.any(RouteEncoder.class), Mockito.anyObject(), Mockito.any(InstanceIdentifier.class), Mockito.eq(this.peer), Mockito.any(Route.class));

        final AdjRIBsIn<?, ?> a2 = this.tables.create(this.trans, this.rib, this.ipv6key);
        assertNotNull(a2);
        mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6CaseBuilder().setDestinationIpv6(new DestinationIpv6Builder().setIpv6Prefixes(
                Lists.newArrayList(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix("2001:db8:1:2::/128")).build())).build()).build()).build());
        a2.addRoutes(this.adjTrans6, this.peer, mpBuilder.build(), paBuilder.build());
        Mockito.verify(this.adjTrans6).advertise(Mockito.any(RouteEncoder.class), Mockito.anyObject(), Mockito.any(InstanceIdentifier.class), Mockito.eq(this.peer), Mockito.any(Route.class));

        final AdjRIBsIn<?, ?> a3 = this.tables.create(this.trans, this.rib, this.linkstateKey);
        assertNull(a3);
    }

    @After
    public void tearDown() {
        this.act.close();
    }

}
