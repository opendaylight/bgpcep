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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4Builder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;


public class AdjacencyRIBsInTest extends AbstractDataBrokerTest {

    private WriteTransaction trans;

    private final DefaultRibReference rib = new DefaultRibReference(InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(new RibId("test"))).toInstance());

    private final RIBActivator act = new RIBActivator();

    private final RIBExtensionProviderContext ctx = new SimpleRIBExtensionProviderContext();

    private AdjRIBsIn<?, ?> a1;

    private RIBTables tables;

    private final TablesKey key = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    @Mock
    AdjRIBsTransaction adjTrans;

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
        Mockito.doReturn(new BGPObjectComparator(new AsNumber(72L))).when(this.adjTrans).comparator();
        Mockito.doReturn("test").when(this.peer).toString();
        Mockito.doNothing().when(this.adjTrans).advertise(Mockito.any(RouteEncoder.class), Mockito.anyObject(), Mockito.any(InstanceIdentifier.class), Mockito.eq(this.peer), Mockito.any(Route.class));
        Mockito.doNothing().when(this.adjTrans).setUptodate(Mockito.any(InstanceIdentifier.class), Mockito.anyBoolean());
    }

    @Test
    public void testRIBTables() {
        this.a1 = this.tables.create(this.trans, this.rib, this.key);
        assertNotNull(this.a1);
        assertEquals(this.a1, this.tables.get(this.key));
    }

    @Test
    public void testAddAdvertisement() {
        this.a1 = this.tables.create(this.trans, this.rib, this.key);
        assertNotNull(this.a1);
        final MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder();
        mpBuilder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(
                Lists.newArrayList(new Ipv4Prefix("127.0.0.1"))).build()).build()).build());
        final PathAttributesBuilder paBuilder = new PathAttributesBuilder();
        this.a1.addRoutes(this.adjTrans, this.peer, mpBuilder.build(), paBuilder.build());
        Mockito.verify(this.adjTrans).advertise(Mockito.any(RouteEncoder.class), Mockito.anyObject(), Mockito.any(InstanceIdentifier.class), Mockito.eq(this.peer), Mockito.any(Route.class));
    }

    @After
    public void tearDown() {
        this.act.close();
    }

}
