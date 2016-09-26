/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javassist.ClassPool;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.yang.bmp.impl.MonitoredRouter;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.RIBActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.BmpActivator;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitoringStation;
import org.opendaylight.protocol.bmp.impl.test.TestUtil;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.tcpmd5.api.KeyAccess;
import org.opendaylight.tcpmd5.api.KeyAccessFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5NioServerSocketChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5NioSocketChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bmp.monitor.monitor.router.peer.pre.policy.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.BmpMonitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.bmp.monitor.Monitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.bmp.monitor.MonitorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.peer.Mirrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.peer.PeerSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.peer.PostPolicyRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.peer.PrePolicyRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.peers.peer.Stats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.routers.RouterKey;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public class BmpMonitorImplTest extends AbstractDataBrokerTest {

    private static final int PORT = 12345;
    private static final String LOCAL_ADDRESS = "127.0.0.10";
    private static final String LOCAL_ADDRESS_2 = "127.0.0.11";
    private static final InetSocketAddress CLIENT_REMOTE = new InetSocketAddress(LOCAL_ADDRESS, PORT);
    private static final InetSocketAddress CLIENT_LOCAL = new InetSocketAddress(LOCAL_ADDRESS, 0);
    private static final Ipv4Address PEER1 = new Ipv4Address("20.20.20.20");
    private static final MonitorId MONITOR_ID = new MonitorId("monitor");
    private static final RouterId ROUTER_ID = new RouterId(new IpAddress(new Ipv4Address(LOCAL_ADDRESS)));
    private static final PeerId PEER_ID = new PeerId(PEER1.getValue());
    private static final String MD5_PASSWORD = "abcdef";

    private BindingToNormalizedNodeCodec mappingService;
    private RIBActivator ribActivator;
    private BGPActivator bgpActivator;
    private BmpActivator bmpActivator;
    private BmpDispatcher dispatcher;
    private BmpMonitoringStation bmpApp;
    private BmpMessageRegistry msgRegistry;
    private MD5NioServerSocketChannelFactory scfServerMd5;
    private MD5NioSocketChannelFactory scfMd5;
    private List<MonitoredRouter> mrs;
    private ModuleInfoBackedContext moduleInfoBackedContext;

    @Mock private KeyAccess mockKeyAccess;
    @Mock private KeyAccessFactory kaf;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.mappingService = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
                new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        this.moduleInfoBackedContext = ModuleInfoBackedContext.create();
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(InitiationMessage.class));
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(CParameters1.class));
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(BgpParameters.class));
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(MultiprotocolCapability.class));
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(DestinationIpv4Case.class));
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(AdvertizedRoutes.class));
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(SentOpen.class));
        this.moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(ReceivedOpen.class));
        this.mappingService.onGlobalContextUpdated(this.moduleInfoBackedContext.tryToCreateSchemaContext().get());

        final KeyMapping keys = new KeyMapping();
        keys.put(InetAddresses.forString(LOCAL_ADDRESS), MD5_PASSWORD.getBytes(Charsets.US_ASCII));

        Mockito.doReturn(this.mockKeyAccess).when(this.kaf).getKeyAccess(Mockito.any(java.nio.channels.Channel.class));
        Mockito.doReturn(keys).when(this.mockKeyAccess).getKeys();
        Mockito.doNothing().when(this.mockKeyAccess).setKeys(Mockito.any(KeyMapping.class));

        this.scfServerMd5 = new MD5NioServerSocketChannelFactory(this.kaf);
        this.scfMd5 = new MD5NioSocketChannelFactory(this.kaf);

        this.ribActivator = new RIBActivator();
        final RIBExtensionProviderContext ribExtension = new SimpleRIBExtensionProviderContext();
        this.ribActivator.startRIBExtensionProvider(ribExtension);

        this.bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        this.bgpActivator.start(context);
        final SimpleBmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        this.bmpActivator = new BmpActivator(context);
        this.bmpActivator.start(ctx);
        this.msgRegistry = ctx.getBmpMessageRegistry();

        this.dispatcher = new BmpDispatcherImpl(new NioEventLoopGroup(), new NioEventLoopGroup(),
                ctx.getBmpMessageRegistry(), new DefaultBmpSessionFactory(), Optional.<MD5ChannelFactory<?>>of(this.scfMd5),
                Optional.<MD5ServerChannelFactory<?>>of(this.scfServerMd5));

        this.bmpApp = BmpMonitoringStationImpl.createBmpMonitorInstance(ribExtension, this.dispatcher, getDomBroker(),
                MONITOR_ID, new InetSocketAddress(InetAddresses.forString("127.0.0.10"), PORT), Optional.of(keys),
                this.mappingService.getCodecFactory(), moduleInfoBackedContext.getSchemaContext(), this.mrs);

        readData(InstanceIdentifier.create(BmpMonitor.class), new Function<BmpMonitor, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final BmpMonitor monitor) {
                Assert.assertEquals(1, monitor.getMonitor().size());
                final Monitor bmpMonitor = monitor.getMonitor().get(0);
                Assert.assertEquals(MONITOR_ID, bmpMonitor.getMonitorId());
                Assert.assertEquals(0, bmpMonitor.getRouter().size());
                return monitor;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        this.ribActivator.close();
        this.bgpActivator.close();
        this.bmpActivator.close();
        this.dispatcher.close();
        this.bmpApp.close();
        this.mappingService.close();

        readData(InstanceIdentifier.create(BmpMonitor.class), new Function<BmpMonitor, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final BmpMonitor monitor) {
                assertTrue(monitor.getMonitor().isEmpty());
                return monitor;
            }
        });
    }

    @Test
    public void testMonitoringStation() throws InterruptedException {
        final Channel channel = connectTestClient(this.msgRegistry).channel();
        try {
            Thread.sleep(500);
            final KeyedInstanceIdentifier<Monitor, MonitorKey> monitorIId = InstanceIdentifier.create(BmpMonitor.class).child(Monitor.class, new MonitorKey(MONITOR_ID));

            readData(monitorIId, new Function<Monitor, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final Monitor monitor) {
                    assertEquals(1, monitor.getRouter().size());
                    final Router router = monitor.getRouter().get(0);
                    assertEquals(ROUTER_ID, router.getRouterId());
                    assertEquals(Status.Down, router.getStatus());
                    assertTrue(router.getPeer().isEmpty());
                    return monitor;
                }
            });
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createInitMsg("description", "name", "some info")));
            readData(monitorIId, new Function<Monitor, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final Monitor monitor) {
                    assertEquals(1, monitor.getRouter().size());
                    final Router routerInit = monitor.getRouter().get(0);
                    assertEquals("some info;", routerInit.getInfo());
                    assertEquals("name", routerInit.getName());
                    assertEquals("description", routerInit.getDescription());
                    assertEquals(ROUTER_ID, routerInit.getRouterId());
                    assertTrue(routerInit.getPeer().isEmpty());
                    assertEquals(Status.Up, routerInit.getStatus());
                    return monitor;
                }
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createPeerUpNotification(PEER1, true)));

            final KeyedInstanceIdentifier<Router, RouterKey> routerIId = monitorIId.child(Router.class, new RouterKey(ROUTER_ID));
            readData(routerIId, new Function<Router, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final Router router) {
                    final List<Peer> peers = router.getPeer();
                    assertEquals(1, peers.size());
                    final Peer peer = peers.get(0);
                    assertEquals(PeerType.Global, peer.getType());
                    assertEquals(PEER_ID, peer.getPeerId());
                    assertEquals(PEER1, peer.getBgpId());
                    assertEquals(TestUtil.IPV4_ADDRESS_10, peer.getAddress().getIpv4Address());
                    assertEquals(TestUtil.PEER_AS, peer.getAs());
                    assertNull(peer.getDistinguisher());
                    assertNull(peer.getStats());

                    assertNotNull(peer.getPrePolicyRib());
                    assertEquals(1, peer.getPrePolicyRib().getTables().size());
                    final Tables prePolicyTable = peer.getPrePolicyRib().getTables().get(0);
                    assertEquals(Ipv4AddressFamily.class, prePolicyTable.getAfi());
                    assertEquals(UnicastSubsequentAddressFamily.class, prePolicyTable.getSafi());
                    assertFalse(prePolicyTable.getAttributes().isUptodate());
                    assertNotNull(prePolicyTable.getRoutes());

                    assertNotNull(peer.getPostPolicyRib());
                    assertEquals(1, peer.getPostPolicyRib().getTables().size());
                    final Tables postPolicyTable = peer.getPrePolicyRib().getTables().get(0);
                    assertEquals(Ipv4AddressFamily.class, postPolicyTable.getAfi());
                    assertEquals(UnicastSubsequentAddressFamily.class, postPolicyTable.getSafi());
                    assertFalse(postPolicyTable.getAttributes().isUptodate());
                    assertNotNull(postPolicyTable.getRoutes());

                    assertNotNull(peer.getPeerSession());
                    final PeerSession peerSession = peer.getPeerSession();
                    assertEquals(TestUtil.IPV4_ADDRESS_10, peerSession.getLocalAddress().getIpv4Address());
                    assertEquals(TestUtil.PEER_LOCAL_PORT, peerSession.getLocalPort());
                    assertEquals(TestUtil.PEER_REMOTE_PORT, peerSession.getRemotePort());
                    assertEquals(Status.Up, peerSession.getStatus());
                    return router;
                }
            });


            final StatsReportsMessage statsMsg = TestUtil.createStatsReportMsg(PEER1);
            waitWriteAndFlushSuccess(channel.writeAndFlush(statsMsg));
            final KeyedInstanceIdentifier<Peer, PeerKey> peerIId = routerIId.child(Peer.class, new PeerKey(PEER_ID));
            readData(peerIId.child(Stats.class), new Function<Stats, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final Stats peerStats) {
                    assertNotNull(peerStats.getTimestampSec());
                    final Tlvs tlvs = statsMsg.getTlvs();
                    assertEquals(tlvs.getAdjRibsInRoutesTlv().getCount(), peerStats.getAdjRibsInRoutes());
                    assertEquals(tlvs.getDuplicatePrefixAdvertisementsTlv().getCount(), peerStats.getDuplicatePrefixAdvertisements());
                    assertEquals(tlvs.getDuplicateWithdrawsTlv().getCount(), peerStats.getDuplicateWithdraws());
                    assertEquals(tlvs.getInvalidatedAsConfedLoopTlv().getCount(), peerStats.getInvalidatedAsConfedLoop());
                    assertEquals(tlvs.getInvalidatedAsPathLoopTlv().getCount(), peerStats.getInvalidatedAsPathLoop());
                    assertEquals(tlvs.getInvalidatedClusterListLoopTlv().getCount(), peerStats.getInvalidatedClusterListLoop());
                    assertEquals(tlvs.getInvalidatedOriginatorIdTlv().getCount(), peerStats.getInvalidatedOriginatorId());
                    assertEquals(tlvs.getLocRibRoutesTlv().getCount(), peerStats.getLocRibRoutes());
                    assertEquals(tlvs.getRejectedPrefixesTlv().getCount(), peerStats.getRejectedPrefixes());
                    assertEquals(tlvs.getPerAfiSafiAdjRibInTlv().getCount().toString(), peerStats.getPerAfiSafiAdjRibInRoutes().getAfiSafi().get(0).getCount().toString());
                    assertEquals(tlvs.getPerAfiSafiLocRibTlv().getCount().toString(), peerStats.getPerAfiSafiLocRibRoutes().getAfiSafi().get(0).getCount().toString());
                    return peerStats;
                }
            });
         // route mirror message test
            final RouteMirroringMessage routeMirrorMsg = TestUtil.createRouteMirrorMsg(PEER1);
            waitWriteAndFlushSuccess(channel.writeAndFlush(routeMirrorMsg));
            readData(peerIId.child(Mirrors.class), new Function<Mirrors, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final Mirrors routeMirrors) {
                    assertNotNull(routeMirrors.getTimestampSec());
                    return routeMirrors;
                }
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonitMsg(false, PEER1, AdjRibInType.PrePolicy)));
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonMsgWithEndOfRibMarker(PEER1, AdjRibInType.PrePolicy)));
            readData(peerIId.child(PrePolicyRib.class), new Function<PrePolicyRib, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final PrePolicyRib prePolicyRib) {
                    final Tables table = prePolicyRib.getTables().get(0);
                    assertTrue(table.getAttributes().isUptodate());
                    assertEquals(3, ((Ipv4RoutesCase) table.getRoutes()).getIpv4Routes().getIpv4Route().size());
                    return prePolicyRib;
                }
            });


            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonitMsg(false, PEER1, AdjRibInType.PostPolicy)));
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonMsgWithEndOfRibMarker(PEER1, AdjRibInType.PostPolicy)));
            readData(peerIId.child(PostPolicyRib.class), new Function<PostPolicyRib, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final PostPolicyRib postPolicyRib) {
                    final Tables tables = postPolicyRib.getTables().get(0);
                    assertTrue(tables.getAttributes().isUptodate());
                    assertEquals(3, ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bmp.monitor.monitor.router.peer.post.policy.rib.tables.routes.Ipv4RoutesCase) tables.getRoutes()).getIpv4Routes().getIpv4Route().size());
                    return postPolicyRib;
                }
            });
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createPeerDownNotification(PEER1)));
            readData(routerIId, new Function<Router, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final Router peersAfterDown) {
                    assertTrue(peersAfterDown.getPeer().isEmpty());
                    return peersAfterDown;
                }
            });

            channel.close().await();
            readData(monitorIId, new Function<Monitor, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable final Monitor monitorAfterClose) {
                    assertTrue(monitorAfterClose.getRouter().isEmpty());
                    return monitorAfterClose;
                }
            });

        } catch (final Exception e) {
            final StringBuffer ex = new StringBuffer();
            ex.append(e.getMessage() + "\n");
            for (final StackTraceElement element: e.getStackTrace()) {
                ex.append(element.toString() + "\n");
            }
            fail(ex.toString());
        }
    }

    @Test
    public void deploySecondInstance() throws Exception {
        final BmpMonitoringStation monitoringStation2 = BmpMonitoringStationImpl.createBmpMonitorInstance(new SimpleRIBExtensionProviderContext(), this.dispatcher, getDomBroker(),
                new MonitorId("monitor2"), new InetSocketAddress(InetAddresses.forString(LOCAL_ADDRESS_2), PORT), Optional.of(new KeyMapping()),
                this.mappingService.getCodecFactory(), this.moduleInfoBackedContext.getSchemaContext(), null);

        readData(InstanceIdentifier.create(BmpMonitor.class), new Function<BmpMonitor, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final BmpMonitor monitor) {
                Assert.assertEquals(2, monitor.getMonitor().size());
                return monitor;
            }
        });

        monitoringStation2.close();
    }

    private ChannelFuture connectTestClient(final BmpMessageRegistry msgRegistry) throws InterruptedException {
        final BmpHandlerFactory hf = new BmpHandlerFactory(msgRegistry);
        final Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup());
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.channelFactory(this.scfMd5);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        b.localAddress(CLIENT_LOCAL);
        b.option(ChannelOption.SO_REUSEADDR, true);
        return b.connect(CLIENT_REMOTE).sync();
    }

    private <T extends DataObject> Optional<T> getBmpData(final InstanceIdentifier<T> iid) throws ReadFailedException {
        try (final ReadOnlyTransaction tx = getDataBroker().newReadOnlyTransaction()) {
            return tx.read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet();
        }
    }

    private <R, T extends DataObject> R readData(final InstanceIdentifier<T> iid, final Function<T, R> function)
        throws ReadFailedException {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 10) {
            try (final ReadOnlyTransaction tx = getDataBroker().newReadOnlyTransaction()) {
                final Optional<T> data = tx.read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet();
                if(data.isPresent()) {
                    try {
                        return function.apply(data.get());
                    } catch (final AssertionError e) {
                        lastError = e;
                        Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }

        throw lastError;
    }

    private static <T extends Future> void waitFutureSuccess(final T future) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        future.addListener(new FutureListener<T>() {
            @Override
            public void operationComplete(final Future future) throws Exception {
                latch.countDown();
            }
        });
        Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.SECONDS);
    }

    private void waitWriteAndFlushSuccess(final ChannelFuture channelFuture) throws InterruptedException {
        waitFutureSuccess(channelFuture);
    }
}
