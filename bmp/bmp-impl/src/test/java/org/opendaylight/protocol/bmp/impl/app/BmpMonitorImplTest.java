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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.net.InetAddresses;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.util.List;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.impl.config.BmpDeployerDependencies;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitoringStation;
import org.opendaylight.protocol.bmp.parser.BmpActivator;
import org.opendaylight.protocol.bmp.parser.message.TestUtil;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.protocol.util.CheckUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.bmp.monitor.monitor.router.peer.pre.policy.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.up.ReceivedOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.up.SentOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.BmpMonitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.bmp.monitor.Monitor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.bmp.monitor.MonitorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.peers.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.peers.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.peers.peer.Mirrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.peers.peer.PeerSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.peers.peer.PostPolicyRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.peers.peer.PrePolicyRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.peers.peer.Stats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.routers.RouterKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class BmpMonitorImplTest extends AbstractConcurrentDataBrokerTest {
    // the local port and address where the monitor (ODL) will listen for incoming BMP request
    private static final int MONITOR_LOCAL_PORT = 12345;
    private static final String MONITOR_LOCAL_ADDRESS = "127.0.0.10";
    private static final String MONITOR_LOCAL_ADDRESS_2 = "127.0.0.11";
    // the router (monitee) address where we are going to simulate a BMP request from
    private static final String REMOTE_ROUTER_ADDRESS_1 = "127.0.0.12";
    private static final String REMOTE_ROUTER_ADDRESS_2 = "127.0.0.13";
    private static final Ipv4Address PEER1 = new Ipv4Address("20.20.20.20");
    private static final MonitorId MONITOR_ID = new MonitorId("monitor");
    private static final KeyedInstanceIdentifier<Monitor, MonitorKey> MONITOR_IID = InstanceIdentifier
        .create(BmpMonitor.class).child(Monitor.class, new MonitorKey(MONITOR_ID));
    private static final PeerId PEER_ID = new PeerId(PEER1.getValue());
    private static final InstanceIdentifier<BmpMonitor> BMP_II = InstanceIdentifier.create(BmpMonitor.class);
    private BindingToNormalizedNodeCodec mappingService;
    private RIBActivator ribActivator;
    private BGPActivator bgpActivator;
    private BmpActivator bmpActivator;
    private BmpDispatcher dispatcher;
    private BmpMonitoringStation bmpApp;
    private BmpMessageRegistry msgRegistry;
    private RIBExtensionProviderContext ribExtension;
    private ClusterSingletonService singletonService;
    private ClusterSingletonService singletonService2;
    @Mock
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    @Mock
    private ClusterSingletonServiceRegistration singletonServiceRegistration2;
    @Mock
    private ClusterSingletonServiceProvider clusterSSProv;
    @Mock
    private ClusterSingletonServiceProvider clusterSSProv2;
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doAnswer(invocationOnMock -> {
            BmpMonitorImplTest.this.singletonService = (ClusterSingletonService) invocationOnMock.getArguments()[0];
            this.singletonService.instantiateServiceInstance();
            return BmpMonitorImplTest.this.singletonServiceRegistration;
        }).when(this.clusterSSProv).registerClusterSingletonService(any(ClusterSingletonService.class));

        doAnswer(invocationOnMock -> BmpMonitorImplTest.this.singletonService.closeServiceInstance())
            .when(this.singletonServiceRegistration).close();

        doAnswer(invocationOnMock -> {
            this.singletonService2 = (ClusterSingletonService) invocationOnMock.getArguments()[0];
            this.singletonService2.instantiateServiceInstance();
            return BmpMonitorImplTest.this.singletonServiceRegistration2;
        }).when(this.clusterSSProv2).registerClusterSingletonService(any(ClusterSingletonService.class));

        doAnswer(invocationOnMock -> BmpMonitorImplTest.this.singletonService2.closeServiceInstance())
            .when(this.singletonServiceRegistration2).close();

        this.mappingService = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
            new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(InitiationMessage.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(CParameters1.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(BgpParameters.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(MultiprotocolCapability.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(DestinationIpv4Case.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(AdvertizedRoutes.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(SentOpen.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(ReceivedOpen.class));
        this.mappingService.onGlobalContextUpdated(moduleInfoBackedContext.tryToCreateSchemaContext().get());
        this.ribActivator = new RIBActivator();
        this.ribExtension = new SimpleRIBExtensionProviderContext();
        this.ribActivator.startRIBExtensionProvider(this.ribExtension);

        this.bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        this.bgpActivator.start(context);
        final SimpleBmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        this.bmpActivator = new BmpActivator(context);
        this.bmpActivator.start(ctx);
        this.msgRegistry = ctx.getBmpMessageRegistry();

        this.dispatcher = new BmpDispatcherImpl(new NioEventLoopGroup(), new NioEventLoopGroup(),
            ctx.getBmpMessageRegistry(), new DefaultBmpSessionFactory());

        final InetSocketAddress inetAddress = new InetSocketAddress(InetAddresses.forString(MONITOR_LOCAL_ADDRESS),
            MONITOR_LOCAL_PORT);

        final DOMDataWriteTransaction wTx = getDomBroker().newWriteOnlyTransaction();
        final ContainerNode parentNode = Builders.containerBuilder().withNodeIdentifier(
            new NodeIdentifier(BmpMonitor.QNAME)).addChild(ImmutableNodes.mapNodeBuilder(Monitor.QNAME).build()).build();
        wTx.merge(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(BmpMonitor.QNAME), parentNode);
        wTx.submit();

        final BmpDeployerDependencies bmpDependecies = new BmpDeployerDependencies(getDataBroker(), getDomBroker(),
            this.ribExtension, this.mappingService.getCodecFactory(), getSchemaContext(), this.clusterSSProv);
        this.bmpApp = new BmpMonitoringStationImpl(bmpDependecies, this.dispatcher, MONITOR_ID, inetAddress, null);
        readDataOperational(getDataBroker(), BMP_II, monitor -> {
            assertEquals(1, monitor.getMonitor().size());
            final Monitor bmpMonitor = monitor.getMonitor().get(0);
            assertEquals(MONITOR_ID, bmpMonitor.getMonitorId());
            assertEquals(0, bmpMonitor.getRouter().size());
            assertEquals(MONITOR_ID, bmpMonitor.getMonitorId());
            assertEquals(0, bmpMonitor.getRouter().size());
            return monitor;
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

        readDataOperational(getDataBroker(), BMP_II, monitor -> {
            assertTrue(monitor.getMonitor().isEmpty());
            return monitor;
        });
    }

    @Test
    public void testRouterMonitoring() throws Exception {
        // first test if a single router monitoring is working
        final Channel channel1 = testMonitoringStation(REMOTE_ROUTER_ADDRESS_1);
        readDataOperational(getDataBroker(), MONITOR_IID, monitor -> {
            assertEquals(1, monitor.getRouter().size());
            return monitor;
        });

        final Channel channel2 = testMonitoringStation(REMOTE_ROUTER_ADDRESS_2);
        readDataOperational(getDataBroker(), MONITOR_IID, monitor -> {
            assertEquals(2, monitor.getRouter().size());
            return monitor;
        });

        // initiate another BMP request from router 1, create a redundant connection
        // we expect the connection to be closed
        final Channel channel3 = connectTestClient(REMOTE_ROUTER_ADDRESS_1, this.msgRegistry);


        // channel 1 should still be open, while channel3 should be closed
        CheckUtil.checkEquals(() -> assertTrue(channel1.isOpen()));
        CheckUtil.checkEquals(() -> assertFalse(channel3.isOpen()));

        // now if we close the channel 1 and try it again, it should succeed
        waitFutureSuccess(channel1.close());

        // channel 2 is still open
        readDataOperational(getDataBroker(), MONITOR_IID, monitor -> {
            assertEquals(1, monitor.getRouter().size());
            return monitor;
        });

        final Channel channel4 = testMonitoringStation(REMOTE_ROUTER_ADDRESS_1);
        readDataOperational(getDataBroker(), MONITOR_IID, monitor -> {
            assertEquals(2, monitor.getRouter().size());
            return monitor;
        });

        // close all channel altogether
        waitFutureSuccess(channel2.close());
        Thread.sleep(100);

        // sleep for a while to avoid intermittent InMemoryDataTree modification conflict
        waitFutureSuccess(channel4.close());

        readDataOperational(getDataBroker(), MONITOR_IID, monitor -> {
            assertEquals(0, monitor.getRouter().size());
            return monitor;
        });
    }

    private static void waitWriteAndFlushSuccess(final ChannelFuture channelFuture) {
        waitFutureSuccess(channelFuture);
    }

    private Channel testMonitoringStation(final String remoteRouterIpAddr) throws InterruptedException {
        final Channel channel = connectTestClient(remoteRouterIpAddr, this.msgRegistry);
        final RouterId routerId = getRouterId(remoteRouterIpAddr);
        try {
            readDataOperational(getDataBroker(), MONITOR_IID, monitor -> {
                assertFalse(monitor.getRouter().isEmpty());
                // now find the current router instance
                Router router = null;
                for (final Router r : monitor.getRouter()) {
                    if (routerId.equals(r.getRouterId())) {
                        router = r;
                        break;
                    }
                }
                assertNotNull(router);
                assertEquals(Status.Down, router.getStatus());
                assertTrue(router.getPeer().isEmpty());
                return router;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil
                    .createInitMsg("description", "name", "some info")));

            readDataOperational(getDataBroker(), MONITOR_IID, monitor -> {
                assertFalse(monitor.getRouter().isEmpty());
                Router retRouter = null;
                for (final Router r : monitor.getRouter()) {
                    if (routerId.equals(r.getRouterId())) {
                        retRouter = r;
                        break;
                    }
                }

                assertEquals("some info;", retRouter.getInfo());
                assertEquals("name", retRouter.getName());
                assertEquals("description", retRouter.getDescription());
                assertEquals(routerId, retRouter.getRouterId());
                assertTrue(retRouter.getPeer().isEmpty());
                assertEquals(Status.Up, retRouter.getStatus());
                return retRouter;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createPeerUpNotification(PEER1, true)));
            final KeyedInstanceIdentifier<Router, RouterKey> routerIId = MONITOR_IID.child(Router.class, new RouterKey(routerId));

            readDataOperational(getDataBroker(), routerIId, router -> {
                final List<Peer> peers = router.getPeer();
                assertEquals(1, peers.size());
                final Peer peer = peers.get(0);
                assertEquals(PeerType.Global, peer.getType());
                assertEquals(PEER_ID, peer.getPeerId());
                assertEquals(PEER1, peer.getBgpId());
                assertEquals(TestUtil.IPV4_ADDRESS_10, peer.getAddress().getIpv4Address());
                assertEquals(TestUtil.PEER_AS, peer.getAs());
                assertNull(peer.getPeerDistinguisher());
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
                assertNotNull(peerSession.getReceivedOpen());
                assertNotNull(peerSession.getSentOpen());
                return router;
            });


            final StatsReportsMessage statsMsg = TestUtil.createStatsReportMsg(PEER1);
            waitWriteAndFlushSuccess(channel.writeAndFlush(statsMsg));
            final KeyedInstanceIdentifier<Peer, PeerKey> peerIId = routerIId.child(Peer.class, new PeerKey(PEER_ID));

            readDataOperational(getDataBroker(), peerIId.child(Stats.class), peerStats -> {
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
            });

            // route mirror message test
            final RouteMirroringMessage routeMirrorMsg = TestUtil.createRouteMirrorMsg(PEER1);
            waitWriteAndFlushSuccess(channel.writeAndFlush(routeMirrorMsg));

            readDataOperational(getDataBroker(), peerIId.child(Mirrors.class), routeMirrors -> {
                assertNotNull(routeMirrors.getTimestampSec());
                return routeMirrors;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonitMsg(false, PEER1, AdjRibInType.PrePolicy)));
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonMsgWithEndOfRibMarker(PEER1, AdjRibInType.PrePolicy)));

            readDataOperational(getDataBroker(), peerIId.child(PrePolicyRib.class), prePolicyRib -> {
                assertTrue(!prePolicyRib.getTables().isEmpty());
                final Tables tables = prePolicyRib.getTables().get(0);
                assertTrue(tables.getAttributes().isUptodate());
                assertEquals(3, ((Ipv4RoutesCase) tables.getRoutes()).getIpv4Routes().getIpv4Route().size());
                return tables;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonitMsg(false, PEER1, AdjRibInType.PostPolicy)));
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonMsgWithEndOfRibMarker(PEER1, AdjRibInType.PostPolicy)));

            readDataOperational(getDataBroker(), peerIId.child(PostPolicyRib.class), postPolicyRib -> {
                assertTrue(!postPolicyRib.getTables().isEmpty());
                final Tables tables = postPolicyRib.getTables().get(0);
                assertTrue(tables.getAttributes().isUptodate());
                assertEquals(3, ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.bmp.monitor.monitor.router.peer.post.policy.rib.tables.routes.Ipv4RoutesCase)
                    tables.getRoutes()).getIpv4Routes().getIpv4Route().size());
                return tables;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createPeerDownNotification(PEER1)));

            readDataOperational(getDataBroker(), routerIId, router -> {
                final List<Peer> peersAfterDown = router.getPeer();
                assertTrue(peersAfterDown.isEmpty());
                return router;
            });
        } catch (final Exception e) {
            final StringBuilder ex = new StringBuilder();
            ex.append(e.getMessage()).append("\n");
            for (final StackTraceElement element : e.getStackTrace()) {
                ex.append(element.toString()).append("\n");
            }
            fail(ex.toString());
        }
        return channel;
    }

    @Test
    public void deploySecondInstance() throws Exception {
        final BmpDeployerDependencies bmpDependecies = new BmpDeployerDependencies(getDataBroker(), getDomBroker(),
            this.ribExtension, this.mappingService.getCodecFactory(), getSchemaContext(), this.clusterSSProv2);

        final BmpMonitoringStation monitoringStation2 = new BmpMonitoringStationImpl(bmpDependecies,
            this.dispatcher, new MonitorId("monitor2"), new InetSocketAddress(InetAddresses.
            forString(MONITOR_LOCAL_ADDRESS_2), MONITOR_LOCAL_PORT), null);

        readDataOperational(getDataBroker(), BMP_II, monitor -> {
            assertEquals(2, monitor.getMonitor().size());
            return monitor;
        });

        monitoringStation2.close();
    }

    private static Channel connectTestClient(final String routerIp, final BmpMessageRegistry msgRegistry)
            throws InterruptedException {
        final BmpHandlerFactory hf = new BmpHandlerFactory(msgRegistry);
        final Bootstrap b = new Bootstrap();
        final EventLoopGroup workerGroup;
        if (Epoll.isAvailable()) {
            b.channel(EpollSocketChannel.class);
            workerGroup = new EpollEventLoopGroup();
        } else {
            b.channel(NioSocketChannel.class);
            workerGroup = new NioEventLoopGroup();
        }
        b.group(workerGroup);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        b.localAddress(new InetSocketAddress(routerIp, 0));
        b.option(ChannelOption.SO_REUSEADDR, true);
        final ChannelFuture future = b.connect(new InetSocketAddress(MONITOR_LOCAL_ADDRESS, MONITOR_LOCAL_PORT)).sync();
        waitFutureSuccess(future);
        return future.channel();
    }

    private static RouterId getRouterId(final String routerIp) {
        return new RouterId(new IpAddress(new Ipv4Address(routerIp)));
    }
}
