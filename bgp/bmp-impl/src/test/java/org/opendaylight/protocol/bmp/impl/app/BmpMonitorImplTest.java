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

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Uninterruptibles;
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
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.BmpHandlerFactory;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitoringStation;
import org.opendaylight.protocol.bmp.parser.BmpActivator;
import org.opendaylight.protocol.bmp.parser.message.TestUtil;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bmp.monitor.monitor.router.peer.pre.policy.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
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
    // the local port and address where the monitor (ODL) will listen for incoming BMP request
    private static final int MONITOR_LOCAL_PORT = 12345;
    private static final String MONITOR_LOCAL_ADDRESS = "127.0.0.10";
    private static final String MONITOR_LOCAL_ADDRESS_2 = "127.0.0.11";
    // the router (monitee) address where we are going to simulate a BMP request from
    private static final String REMOTE_ROUTER_ADDRESS_1 = "127.0.0.12";
    private static final String REMOTE_ROUTER_ADDRESS_2 = "127.0.0.13";
    private static final Ipv4Address PEER1 = new Ipv4Address("20.20.20.20");
    private static final MonitorId MONITOR_ID = new MonitorId("monitor");
    private static final KeyedInstanceIdentifier<Monitor, MonitorKey> MONITOR_IID = InstanceIdentifier.create(BmpMonitor.class).child(Monitor.class, new MonitorKey(MONITOR_ID));
    private static final PeerId PEER_ID = new PeerId(PEER1.getValue());
    private static final String MD5_PASSWORD = "abcdef";

    private BindingToNormalizedNodeCodec mappingService;
    private RIBActivator ribActivator;
    private BGPActivator bgpActivator;
    private BmpActivator bmpActivator;
    private BmpDispatcher dispatcher;
    private BmpMonitoringStation bmpApp;
    private BmpMessageRegistry msgRegistry;
    private ModuleInfoBackedContext moduleInfoBackedContext;


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

        final KeyMapping keys = KeyMapping.getKeyMapping(InetAddresses.forString(MONITOR_LOCAL_ADDRESS), MD5_PASSWORD);
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
                ctx.getBmpMessageRegistry(), new DefaultBmpSessionFactory());

        this.bmpApp = BmpMonitoringStationImpl.createBmpMonitorInstance(ribExtension, this.dispatcher, getDomBroker(),
                MONITOR_ID, new InetSocketAddress(InetAddresses.forString(MONITOR_LOCAL_ADDRESS), MONITOR_LOCAL_PORT), Optional.of(keys),
                this.mappingService.getCodecFactory(), this.moduleInfoBackedContext.getSchemaContext(), null);

        readData(InstanceIdentifier.create(BmpMonitor.class), monitor -> {
            Assert.assertEquals(1, monitor.getMonitor().size());
            final Monitor bmpMonitor = monitor.getMonitor().get(0);
            Assert.assertEquals(MONITOR_ID, bmpMonitor.getMonitorId());
            Assert.assertEquals(0, bmpMonitor.getRouter().size());
            Assert.assertEquals(MONITOR_ID, bmpMonitor.getMonitorId());
            Assert.assertEquals(0, bmpMonitor.getRouter().size());
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

        readData(InstanceIdentifier.create(BmpMonitor.class), monitor -> {
            assertTrue(monitor.getMonitor().isEmpty());
            return monitor;
        });
    }

    @Test
    public void testRouterMonitoring() throws Exception {
        // first test if a single router monitoring is working
        final Channel channel1 = testMonitoringStation(REMOTE_ROUTER_ADDRESS_1);
        readData(MONITOR_IID, monitor -> {
            assertEquals(1, monitor.getRouter().size());
            return monitor;
        });

        final Channel channel2 = testMonitoringStation(REMOTE_ROUTER_ADDRESS_2);
        readData(MONITOR_IID, monitor -> {
            assertEquals(2, monitor.getRouter().size());
            return monitor;
        });

        // initiate another BMP request from router 1, create a redundant connection
        // we expect the connection to be closed
        final Channel channel3 = connectTestClient(REMOTE_ROUTER_ADDRESS_1, this.msgRegistry);

        Thread.sleep(500);

        // channel 1 should still be open, while channel3 should be closed
        assertTrue(channel1.isOpen());
        assertFalse(channel3.isOpen());

        // now if we close the channel 1 and try it again, it should succeed
        waitFutureSuccess(channel1.close());

        // channel 2 is still open
        readData(MONITOR_IID, monitor -> {
            assertEquals(1, monitor.getRouter().size());
            return monitor;
        });

        final Channel channel4 = testMonitoringStation(REMOTE_ROUTER_ADDRESS_1);
        readData(MONITOR_IID, monitor -> {
            assertEquals(2, monitor.getRouter().size());
            return monitor;
        });

        // close all channel altogether
        waitFutureSuccess(channel2.close());
        Thread.sleep(500);

        // sleep for a while to avoid intermittent InMemoryDataTree modification conflict
        waitFutureSuccess(channel4.close());

        readData(MONITOR_IID, monitor -> {
            assertEquals(0, monitor.getRouter().size());
            return monitor;
        });
    }

    private static <T extends Future> void waitFutureSuccess(final T future) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        future.addListener(future1 -> latch.countDown());
        Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.SECONDS);
    }

    private void waitWriteAndFlushSuccess(final ChannelFuture channelFuture) throws InterruptedException {
        waitFutureSuccess(channelFuture);
    }

    private Channel testMonitoringStation(final String remoteRouterIpAddr) throws InterruptedException {
        final Channel channel = connectTestClient(remoteRouterIpAddr, this.msgRegistry);
        final RouterId routerId = getRouterId(remoteRouterIpAddr);
        try {
            readData(MONITOR_IID, monitor -> {
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

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createInitMsg("description", "name", "some info")));

            readData(MONITOR_IID, monitor -> {
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

            readData(routerIId, router -> {
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
                assertNotNull(peerSession.getReceivedOpen());
                assertNotNull(peerSession.getSentOpen());
                return router;
            });


            final StatsReportsMessage statsMsg = TestUtil.createStatsReportMsg(PEER1);
            waitWriteAndFlushSuccess(channel.writeAndFlush(statsMsg));
            final KeyedInstanceIdentifier<Peer, PeerKey> peerIId = routerIId.child(Peer.class, new PeerKey(PEER_ID));

            readData(peerIId.child(Stats.class), peerStats -> {
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

            readData(peerIId.child(Mirrors.class), routeMirrors -> {
                assertNotNull(routeMirrors.getTimestampSec());
                return routeMirrors;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonitMsg(false, PEER1, AdjRibInType.PrePolicy)));
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonMsgWithEndOfRibMarker(PEER1, AdjRibInType.PrePolicy)));

            readData(peerIId.child(PrePolicyRib.class), prePolicyRib -> {
                assertTrue(!prePolicyRib.getTables().isEmpty());
                final Tables tables = prePolicyRib.getTables().get(0);
                assertTrue(tables.getAttributes().isUptodate());
                assertEquals(3, ((Ipv4RoutesCase) tables.getRoutes()).getIpv4Routes().getIpv4Route().size());
                return tables;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonitMsg(false, PEER1, AdjRibInType.PostPolicy)));
            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createRouteMonMsgWithEndOfRibMarker(PEER1, AdjRibInType.PostPolicy)));

            readData(peerIId.child(PostPolicyRib.class), postPolicyRib -> {
                assertTrue(!postPolicyRib.getTables().isEmpty());
                final Tables tables = postPolicyRib.getTables().get(0);
                assertTrue(tables.getAttributes().isUptodate());
                assertEquals(3, ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bmp.monitor.monitor.router.peer.post.policy.rib.tables.routes.Ipv4RoutesCase)
                        tables.getRoutes()).getIpv4Routes().getIpv4Route().size());
                return tables;
            });

            waitWriteAndFlushSuccess(channel.writeAndFlush(TestUtil.createPeerDownNotification(PEER1)));

            readData(routerIId, router -> {
                final List<Peer> peersAfterDown = router.getPeer();
                assertTrue(peersAfterDown.isEmpty());
                return router;
            });
        } catch (final Exception e) {
            final StringBuffer ex = new StringBuffer();
            ex.append(e.getMessage()).append("\n");
            for (final StackTraceElement element : e.getStackTrace()) {
                ex.append(element.toString() + "\n");
            }
            fail(ex.toString());
        }
        return channel;
    }

    @Test
    public void deploySecondInstance() throws Exception {
        final BmpMonitoringStation monitoringStation2 = BmpMonitoringStationImpl.createBmpMonitorInstance(new SimpleRIBExtensionProviderContext(), this.dispatcher, getDomBroker(),
                new MonitorId("monitor2"), new InetSocketAddress(InetAddresses.forString(MONITOR_LOCAL_ADDRESS_2), MONITOR_LOCAL_PORT), Optional.of(KeyMapping.getKeyMapping()),
                this.mappingService.getCodecFactory(), this.moduleInfoBackedContext.getSchemaContext(), null);

        readData(InstanceIdentifier.create(BmpMonitor.class), monitor -> {
            Assert.assertEquals(2, monitor.getMonitor().size());
            return monitor;
        });

        monitoringStation2.close();
    }

    private Channel connectTestClient(final String routerIp, final BmpMessageRegistry msgRegistry) throws InterruptedException {
        final BmpHandlerFactory hf = new BmpHandlerFactory(msgRegistry);
        final Bootstrap b = new Bootstrap();
        final EventLoopGroup workerGroup;
        if(Epoll.isAvailable()){
            b.channel(EpollSocketChannel.class);
            workerGroup =new EpollEventLoopGroup();
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

    private RouterId getRouterId(final String routerIp) {
        return new RouterId(new IpAddress(new Ipv4Address(routerIp)));
    }
}
