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
import static org.mockito.Matchers.any;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;

public class ApplicationPeerTest {

    private final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final TablesKey lk = new TablesKey(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);

    private RIBImpl r;

    @Mock
    BGPDispatcher dispatcher;

    @Mock
    ReconnectStrategyFactory tcpStrategyFactory;

    @Mock
    DataBroker dps;

    @Mock
    WriteTransaction transWrite;

    @Mock
    BindingTransactionChain chain;

    @Mock
    ApplicationPeer peer;

    @Mock
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;

    @Mock
    CheckedFuture<?,?> future;

    @Mock
    Optional<Rib> o;

    BGPSessionImpl session;

    List<InstanceIdentifier<?>> routes;

    private BGPPeer classic;

    @Mock
    Channel channel;

    @Mock
    private EventLoop eventLoop;

    private final byte[] linkNlri = new byte[] { 0, 2, 0, 0x65, 2, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, (byte) 0x1a,
        2, 0, 0, 4, 0, 0, 0, 0x48, 2, 1, 0, 4, 0x28, 0x28, 0x28, 0x28, 2, 3, 0, 6, 0, 0, 0, 0, 0, 0x42, 1, 1, 0, 0x18,
        2, 0, 0, 4, 0, 0, 0, 0x48, 2, 1, 0, 4, 0x28, 0x28, 0x28, 0x28, 2, 3, 0, 4, 0, 0, 0, 0x40, 1, 2, 0, 8, 1, 2, 3,
        4, 0x0a, 0x0b, 0x0c, 0x0d, 1, 3, 0, 4, (byte) 0xc5, 0x14, (byte) 0xa0, (byte) 0x2a, 1, 4, 0, 4, (byte) 0xc5,
        0x14, (byte) 0xa0, 0x28, 1, 7, 0, 2, 0, 3};

    private RIBActivator a1;
    private org.opendaylight.protocol.bgp.linkstate.RIBActivator a2;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws InterruptedException, ExecutionException, ReadFailedException {
        MockitoAnnotations.initMocks(this);
        final List<BgpTableType> localTables = new ArrayList<>();
        this.routes = new ArrayList<>();
        localTables.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        localTables.add(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        this.a1 = new RIBActivator();
        this.a1.startRIBExtensionProvider(context);
        this.a2 = new org.opendaylight.protocol.bgp.linkstate.RIBActivator();
        this.a2.startRIBExtensionProvider(context);
        Mockito.doReturn(this.chain).when(this.dps).createTransactionChain(Mockito.any(RIBImpl.class));
        Mockito.doReturn(this.o).when(this.future).get();
        Mockito.doNothing().when(this.transWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class), Mockito.any(Rib.class));
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                ApplicationPeerTest.this.routes.add((InstanceIdentifier<?>) args[1]);
                return args[1];
            }
        }).when(this.transWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class), Mockito.any(Route.class), Mockito.eq(true));
        Mockito.doNothing().when(this.transWrite).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class), Mockito.any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes.class));
        Mockito.doReturn(false).when(this.o).isPresent();
        Mockito.doReturn(this.future).when(this.transWrite).submit();
        Mockito.doNothing().when(this.future).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doReturn(this.transWrite).when(this.chain).newWriteOnlyTransaction();
        Mockito.doReturn(this.eventLoop).when(this.channel).eventLoop();
        Mockito.doReturn("channel").when(this.channel).toString();
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                ApplicationPeerTest.this.routes.remove(args[1]);
                return args[1];
            }
        }).when(this.transWrite).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class));
        this.r = new RIBImpl(new RibId("test"), new AsNumber(5L), new Ipv4Address("127.0.0.1"),
            context , this.dispatcher, this.tcpStrategyFactory, this.tcpStrategyFactory, this.dps, localTables);
        this.peer = new ApplicationPeer(new ApplicationRibId("t"), new Ipv4Address("127.0.0.1"), this.r);
        final ReadOnlyTransaction readTx = Mockito.mock(ReadOnlyTransaction.class);
        Mockito.doReturn(readTx).when(this.dps).newReadOnlyTransaction();
        final CheckedFuture<Optional<DataObject>, ReadFailedException> readFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(Optional.<DataObject>absent()).when(readFuture).checkedGet();
        Mockito.doReturn(readFuture).when(readTx).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class));
    }

    @After
    public void tearDown() {
        this.a1.close();
        this.a2.close();
    }

    @Test
    public void testOnDataChanged() {
        final Map<InstanceIdentifier<?>, DataObject> created = new HashMap<>();
        final AdjRIBsIn<Ipv4Prefix, Ipv4Route> a4 = this.r.getTable(this.tk);
        assertNotNull(a4);

        InstanceIdentifier<?> iid = InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(new RibId("foo")))
            .child(LocRib.class).child(Tables.class, this.tk)
            .child(Ipv4Routes.class).child(Ipv4Route.class, new Ipv4RouteKey(new Ipv4Prefix("127.0.0.1/32")));
        created.put(iid, new Ipv4RouteBuilder().setPrefix(new Ipv4Prefix("127.0.0.1/32")).setAttributes(new AttributesBuilder().build()).build());

        final Map<InstanceIdentifier<?>, DataObject> updated = new HashMap<>();
        final AdjRIBsIn<CLinkstateDestination, LinkstateRoute> al = this.r.getTable(this.lk);
        assertNotNull(al);

        iid = InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(new RibId("foo")))
            .child(LocRib.class).child(Tables.class, this.lk)
            .child((Class)LinkstateRoutes.class).child(LinkstateRoute.class, new LinkstateRouteKey(this.linkNlri));
        final LocalNodeDescriptors lnd = new LocalNodeDescriptorsBuilder().setAsNumber(new AsNumber(72L)).setDomainId(new DomainIdentifier(673720360L))
            .setCRouterIdentifier(new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(new byte[] { 0, 0, 0, 0, 0, 66 })).build()).build()).build();
        final RemoteNodeDescriptors rnd = new RemoteNodeDescriptorsBuilder().setAsNumber(new AsNumber(72L)).setDomainId(new DomainIdentifier(673720360L))
            .setCRouterIdentifier(new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder().setOspfRouterId(64L).build()).build()).build();
        updated.put(iid, new LinkstateRouteBuilder().setProtocolId(ProtocolId.IsisLevel2).setIdentifier(new Identifier(BigInteger.ONE)).setObjectType(
            new LinkCaseBuilder().setLinkDescriptors(new LinkDescriptorsBuilder().setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier(new Ipv4Address("197.20.160.42")))
                .setIpv4NeighborAddress(new Ipv4InterfaceIdentifier(new Ipv4Address("197.20.160.40"))).setLinkLocalIdentifier(16909060L).setLinkRemoteIdentifier(168496141L).setMultiTopologyId(new TopologyIdentifier(3)).build())
                .setLocalNodeDescriptors(lnd)
                .setRemoteNodeDescriptors(rnd).build()).build());

        final Set<InstanceIdentifier<?>> removed = new HashSet<>();
        removed.add(iid);

        Mockito.doReturn(created).when(this.change).getCreatedData();
        Mockito.doReturn(updated).when(this.change).getUpdatedData();
        Mockito.doReturn(Collections.EMPTY_SET).when(this.change).getRemovedPaths();
        this.peer.onDataChanged(this.change);
        assertEquals(3, this.routes.size());

        Mockito.doReturn(Collections.EMPTY_MAP).when(this.change).getCreatedData();
        Mockito.doReturn(Collections.EMPTY_MAP).when(this.change).getUpdatedData();
        Mockito.doReturn(removed).when(this.change).getRemovedPaths();
        this.peer.onDataChanged(this.change);
        assertEquals(2, this.routes.size());
    }

    @Test
    public void testClassicPeer() {
        this.classic = new BGPPeer("testPeer", this.r);
        Mockito.doReturn(null).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        Mockito.doReturn(Boolean.TRUE).when(this.channel).isWritable();
        Mockito.doReturn(null).when(this.channel).close();
        Mockito.doReturn(new DefaultChannelPromise(this.channel)).when(this.channel).writeAndFlush(any(Notification.class));

        Mockito.doReturn(new InetSocketAddress("localhost", 12345)).when(this.channel).remoteAddress();
        Mockito.doReturn(new InetSocketAddress("localhost", 12345)).when(this.channel).localAddress();
        final List<BgpParameters> params = Lists.newArrayList(new BgpParametersBuilder().setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder().setCParameters(new MultiprotocolCaseBuilder()
            .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build())).build());
        this.session = new BGPSessionImpl(this.classic, this.channel, new OpenBuilder().setBgpIdentifier(new Ipv4Address("1.1.1.1")).setHoldTimer(50).setMyAsNumber(72).setBgpParameters(params).build(), 30, null);
        assertEquals(this.r, this.classic.getRib());
        assertEquals("testPeer", this.classic.getName());
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                ((SessionRIBsOut)args[0]).run();
                return null;
            }
        }).when(this.eventLoop).submit(Mockito.any(Runnable.class));
        this.classic.onSessionUp(this.session);
        Assert.assertArrayEquals(new byte[] {1, 1, 1, 1}, this.classic.getRawIdentifier());
        assertEquals("BGPPeer{name=testPeer, tables=[TablesKey [_afi=class org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily, _safi=class org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily]]}", this.classic.toString());
        final List<Ipv4Prefix> prefs = Lists.newArrayList(new Ipv4Prefix("127.0.0.1/32"), new Ipv4Prefix("2.2.2.2/24"));
        final UpdateBuilder ub = new UpdateBuilder();
        ub.setNlri(new NlriBuilder().setNlri(prefs).build());
        ub.setPathAttributes(new PathAttributesBuilder().build());
        this.classic.onMessage(this.session, ub.build());
        assertEquals(3, this.routes.size());

        //create new peer so that it gets advertized routes from RIB
        try (final BGPPeer testingPeer = new BGPPeer("testingPeer", this.r)) {
            testingPeer.onSessionUp(this.session);
            assertEquals(3, this.routes.size());
            assertEquals(1, testingPeer.getBgpPeerState().getSessionEstablishedCount().intValue());
            assertEquals(1, testingPeer.getBgpPeerState().getRouteTable().size());
            assertNotNull(testingPeer.getBgpSessionState());
        }

        ub.setNlri(null);
        ub.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(prefs).build());
        this.classic.onMessage(this.session, ub.build());
        assertEquals(3, this.routes.size());
        this.classic.onMessage(this.session, new KeepaliveBuilder().build());
        this.classic.onMessage(this.session, new UpdateBuilder().setPathAttributes(
            new PathAttributesBuilder().addAugmentation(
                    PathAttributes2.class,
                    new PathAttributes2Builder().setMpUnreachNlri(
                            new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build()).build());
        this.classic.releaseConnection();
    }

    @Test
    public void testClose() {
        final AdjRIBsIn<Ipv4Prefix, Ipv4Route> a4 = this.r.getTable(this.tk);
        assertNotNull(a4);
        this.peer.close();
    }
}
