/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInTransaction;
import org.opendaylight.protocol.bgp.rib.spi.BGPObjectComparator;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class LinkstateAdjRIBsInTest {

    private static final AsNumber TEST_AS_NUMBER = new AsNumber(35L);

    @Mock
    private RibReference rib;

    @Mock
    private Peer peer;

    @Mock
    private AdjRIBsInTransaction adjRibTx;

    private LinkstateAdjRIBsIn lrib;

    private CLinkstateDestinationBuilder dBuilder;

    private final MpReachNlriBuilder builder = new MpReachNlriBuilder();

    private final List<CLinkstateDestination> destinations = new ArrayList<>();

    private final HashMap<InstanceIdentifier<?>, DataObject> data = new HashMap<>();

    private final BGPObjectComparator bgpComparator = new BGPObjectComparator(TEST_AS_NUMBER);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final InstanceIdentifier<Rib> iid = InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(new RibId("test-rib"))).toInstance();
        final KeyedInstanceIdentifier<Tables, TablesKey> key = iid.child(LocRib.class).child(Tables.class, new TablesKey(LinkstateAddressFamily.class,
                LinkstateSubsequentAddressFamily.class));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                LinkstateAdjRIBsInTest.this.data.put((InstanceIdentifier<?>)args[0], (DataObject)args[1]);
                return null;
            }

        }).when(this.adjRibTx).advertise(Mockito.<InstanceIdentifier<DataObject>>any(), Mockito.any(DataObject.class));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                LinkstateAdjRIBsInTest.this.data.remove(args[0]);
                return null;
            }

        }).when(this.adjRibTx).withdraw(Matchers.any(InstanceIdentifier.class));

        Mockito.doAnswer(new Answer<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final InstanceIdentifier<Tables> base = (InstanceIdentifier<Tables>) args[0];
                final Tables tables = (Tables) LinkstateAdjRIBsInTest.this.data.get(key);
                if(tables != null) {
                    LinkstateAdjRIBsInTest.this.data.put(base,
                            new TablesBuilder(tables).setAttributes(new AttributesBuilder().setUptodate((Boolean) args[1]).build()).build());
                }
                return null;
            }
        }).when(this.adjRibTx).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Mockito.anyBoolean());

        Mockito.doReturn(bgpComparator).when(this.adjRibTx).comparator();

        Mockito.doReturn(iid).when(this.rib).getInstanceIdentifier();
        Mockito.doReturn("test").when(this.peer).toString();
        this.lrib = new LinkstateAdjRIBsIn(key);

        this.dBuilder = new CLinkstateDestinationBuilder();

        this.dBuilder.setProtocolId(ProtocolId.Direct);
        this.dBuilder.setIdentifier(new Identifier(new BigInteger(new byte[] { 5 })));
        this.dBuilder.setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder().setAsNumber(TEST_AS_NUMBER).build());
    }

    @Test
    public void testAddPrefix() {
        this.dBuilder.setNlriType(NlriType.Ipv4Prefix);
        this.dBuilder.setPrefixDescriptors(new PrefixDescriptorsBuilder().setIpReachabilityInformation(
                new IpPrefix(new Ipv4Prefix("12.34.35.55/32"))).build());
        this.destinations.add(this.dBuilder.build());
        this.builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                        new DestinationLinkstateBuilder().setCLinkstateDestination(this.destinations).build()).build()).build());

        PathAttributesBuilder pa = new PathAttributesBuilder();
        pa.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());

        this.lrib.addRoutes(this.adjRibTx, this.peer, this.builder.build(), pa.build());

        Mockito.verify(this.adjRibTx, Mockito.times(1)).advertise(Matchers.<InstanceIdentifier<DataObject>>any(), Matchers.any(DataObject.class));
        Mockito.verify(this.adjRibTx, Mockito.times(1)).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Matchers.anyBoolean());

        assertEquals(1, this.data.size());
    }

    @Test
    public void testAddNode() {
        this.dBuilder.setNlriType(NlriType.Node);
        this.dBuilder.setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder().setCRouterIdentifier(
                new IsisPseudonodeCaseBuilder().setIsisPseudonode(
                        new IsisPseudonodeBuilder().setIsIsRouterIdentifier(
                                new IsIsRouterIdentifierBuilder().setIsoSystemId(new IsoSystemIdentifier(new byte[] { 1, 2, 3, 4, 5, 6 })).build()).build()).build()).build());
        this.destinations.add(this.dBuilder.build());
        this.builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                        new DestinationLinkstateBuilder().setCLinkstateDestination(this.destinations).build()).build()).build());

        final PathAttributesBuilder pa = new PathAttributesBuilder();
        pa.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());

        this.lrib.addRoutes(this.adjRibTx, this.peer, this.builder.build(), pa.build());

        Mockito.verify(this.adjRibTx, Mockito.times(1)).advertise(Matchers.<InstanceIdentifier<DataObject>>any(), Matchers.any(DataObject.class));
        Mockito.verify(this.adjRibTx, Mockito.times(1)).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Matchers.anyBoolean());
        assertEquals(1, this.data.size());
    }

    @Test
    public void testAddRemoveLink() {
        this.dBuilder.setNlriType(NlriType.Link);
        this.dBuilder.setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder().setCRouterIdentifier(
                new IsisPseudonodeCaseBuilder().setIsisPseudonode(
                        new IsisPseudonodeBuilder().setIsIsRouterIdentifier(
                                new IsIsRouterIdentifierBuilder().setIsoSystemId(new IsoSystemIdentifier(new byte[] { 1, 2, 3, 4, 5, 6 })).build()).build()).build()).build());
        this.dBuilder.setLinkDescriptors(new LinkDescriptorsBuilder().setIpv4InterfaceAddress(
                new Ipv4InterfaceIdentifier(new Ipv4Address("127.0.0.1"))).build());
        this.destinations.add(this.dBuilder.build());
        this.builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                        new DestinationLinkstateBuilder().setCLinkstateDestination(this.destinations).build()).build()).build());

        final PathAttributesBuilder pa = new PathAttributesBuilder();
        pa.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());

        this.lrib.addRoutes(this.adjRibTx, this.peer, this.builder.build(), pa.build());

        Mockito.verify(this.adjRibTx, Mockito.times(1)).advertise(Matchers.<InstanceIdentifier<DataObject>>any(), Matchers.any(DataObject.class));
        Mockito.verify(this.adjRibTx, Mockito.times(1)).setUptodate(Matchers.<InstanceIdentifier<Tables>>any(), Matchers.anyBoolean());

        assertEquals(1, this.data.size());

        final MpUnreachNlriBuilder builder = new MpUnreachNlriBuilder();
        builder.setAfi(LinkstateAddressFamily.class);
        builder.setSafi(LinkstateSubsequentAddressFamily.class);
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
                                this.destinations).build()).build()).build());
        this.lrib.removeRoutes(this.adjRibTx, this.peer, builder.build());

        Mockito.verify(this.adjRibTx, Mockito.times(1)).withdraw(Matchers.any(InstanceIdentifier.class));
        assertEquals(0, this.data.size());
    }
}
