/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class AddPathTest extends AbstractDataBrokerTest {

    private static final String RIB_ID = "127.0.0.1";
    private static final String PEER1 = "127.0.0.2";
    private static final String PEER2 = "127.0.0.3";
    private static final String PEER3 = "127.0.0.4";
    private static final String PEER4 = "127.0.0.5";

    private static final AsNumber AS_NUMBER = new AsNumber(72L);
    private static final int HOLDTIMER = 180;
    private static final int PORT = 1790;

    private static final String PREFIX1 = "1.1.1.1/32";
    private static final String NH1 = "1.1.1.1";
    private static final String NH2 = "2.2.2.2";

    private RIBActivator ribActivator;
    private BGPActivator bgpActivator;
    private ReconnectStrategyFactory neverReconnectStrategyFactory;
    private BindingToNormalizedNodeCodec mappingService;
    private BGPDispatcherImpl dispatcher;
    private NioEventLoopGroup worker;
    private NioEventLoopGroup boss;
    private RIBExtensionProviderContext ribExtension;
    private BGPExtensionProviderContext context;
    private SchemaContext schemaContext;

    @Before
    public void setUp() throws Exception {
        this.ribActivator = new RIBActivator();
        this.ribExtension = new SimpleRIBExtensionProviderContext();
        this.ribActivator.startRIBExtensionProvider(this.ribExtension);

        this.bgpActivator = new BGPActivator();
        this.context = new SimpleBGPExtensionProviderContext();
        this.bgpActivator.start(this.context);

        this.neverReconnectStrategyFactory = new ReconnectStrategyFactory() {
            @Override
            public ReconnectStrategy createReconnectStrategy() {
                return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 2000);
            }
        };

        this.mappingService = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
                new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(BgpParameters.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(MultiprotocolCapability.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(DestinationIpv4Case.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(AdvertizedRoutes.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(BgpRib.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Attributes1.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(MpReachNlri.class));
        this.mappingService.onGlobalContextUpdated(moduleInfoBackedContext.tryToCreateSchemaContext().get());
        this.schemaContext = moduleInfoBackedContext.getSchemaContext();

        this.worker = new NioEventLoopGroup();
        this.boss = new NioEventLoopGroup();
        this.dispatcher = new BGPDispatcherImpl(context.getMessageRegistry(), this.boss, this.worker);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        this.dispatcher.close();
        this.worker.shutdownGracefully().get();
        this.boss.shutdownGracefully().get();
        this.mappingService.close();
        this.ribActivator.close();
        this.bgpActivator.close();
    }

    /*
     *                                          ___________________
     *                                         | ODL BGP 127.0.0.1 |
     * [peer://127.0.0.2; p1, nh1] --(iBGP)--> |                   | --(RR-client, non add-path) --> [Peer://127.0.0.4; (p1, nh1)]
     *                                         |                   |
     * [peer://127.0.0.3; p1, nh2] --(iBGP)--> |                   | --(RR-client, add-path) --> [Peer://127.0.0.5; (p1, path-id1, nh1), (p1, path-id2, nh2)]
     *                                         |___________________|
     * p1 = 1.1.1.1/32
     * nh1 = 1.1.1.1
     * nh2 = 2.2.2.2
     */
    @Test
    public void testUseCase1() throws Exception {

        //FIXME support add-path
        final RIBImpl ribImpl = new RIBImpl(new RibId("test-rib"), AS_NUMBER, new Ipv4Address(RIB_ID), new Ipv4Address(RIB_ID), this.ribExtension,
                this.dispatcher, this.neverReconnectStrategyFactory, this.mappingService.getCodecFactory(), this.neverReconnectStrategyFactory,
                getDataBroker(), getDomBroker(), Lists.newArrayList(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)),
                this.ribExtension.getClassLoadingStrategy());
        ribImpl.onGlobalContextUpdated(this.schemaContext);

        this.dispatcher.createServer(StrictBGPPeerRegistry.GLOBAL, new InetSocketAddress(RIB_ID, PORT)).sync();
        Thread.sleep(1000);

        final BGPHandlerFactory hf = new BGPHandlerFactory(this.context.getMessageRegistry());
        final BgpParameters nonAddPathParams = createParameter(false);
        final BgpParameters addPathParams = createParameter(true);

        configurePeer(PEER1, ribImpl, nonAddPathParams, PeerRole.Ibgp);
        final Channel session1 = connectPeer(PEER1, ribImpl, nonAddPathParams, this.dispatcher, hf, new SimpleSessionListener());

        configurePeer(PEER2, ribImpl, nonAddPathParams, PeerRole.Ibgp);
        final Channel session2 = connectPeer(PEER2, ribImpl, nonAddPathParams, this.dispatcher, hf, new SimpleSessionListener());

        final SimpleSessionListener session3 = new SimpleSessionListener();
        configurePeer(PEER3, ribImpl, nonAddPathParams, PeerRole.RrClient);
        connectPeer(PEER3, ribImpl, nonAddPathParams, this.dispatcher, hf, session3);

        configurePeer(PEER4, ribImpl, addPathParams, PeerRole.RrClient);
        final Channel session4 = connectPeer(PEER4, ribImpl, addPathParams, this.dispatcher, hf, new SimpleSessionListener());
        //not able to parse add-path NLRIs now, the session will fails
        //replace message handler and session handler with some "packet capture"
        final HexDumpCollector messageCollector = new HexDumpCollector();
        session4.pipeline().remove(BGPByteToMessageDecoder.class);
        session4.pipeline().replace(BGPSessionImpl.class, "message-collector", messageCollector);
        Thread.sleep(500);

        session1.writeAndFlush(createSimpleUpdate(PREFIX1, NH1));
        session2.writeAndFlush(createSimpleUpdate(PREFIX1, NH2));
        Thread.sleep(500);

        final ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        final BgpRib bgpRib = rTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(BgpRib.class)).get().get();
        rTx.close();

        //check 4 peers present in the DS
        Assert.assertEquals(4, bgpRib.getRib().get(0).getPeer().size());
        //check loc-rib - 2 paths are present
        Assert.assertEquals(2, ((Ipv4RoutesCase)bgpRib.getRib().get(0).getLocRib().getTables().get(0).getRoutes()).getIpv4Routes().getIpv4Route().size());
        //check peer's rib-out
        for (final Peer peer : bgpRib.getRib().get(0).getPeer()) {
            if (peer.getPeerId().equals(getPeerId(PEER1))) {
                Assert.assertEquals(0, getPeerRibOutSize(peer));
            } else if (peer.getPeerId().equals(getPeerId(PEER2))) {
                Assert.assertEquals(0, getPeerRibOutSize(peer));
            } else if (peer.getPeerId().equals(getPeerId(PEER3))) {
                Assert.assertEquals(1, getPeerRibOutSize(peer));
            } else if (peer.getPeerId().equals(getPeerId(PEER4))) {
                Assert.assertEquals(2, getPeerRibOutSize(peer));
            } else {
                Assert.fail("Failed to verify " + peer);
            }
        }
        //check received Update message - 2 messages?
        Assert.assertEquals(2, messageCollector.getReceivedMessages().size());
        //TODO check the message hex-dump
    }

    private static int getPeerRibOutSize(final Peer peer) {
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.peer.adj.rib.out.tables.routes.Ipv4RoutesCase)peer.getAdjRibOut().getTables().get(0).getRoutes()).getIpv4Routes().getIpv4Route().size();
    }

    private static PeerId getPeerId(final String peerIp) {
        return new PeerId("bgp://" + peerIp);
    }

    private static ChannelFuture createClient(final BGPDispatcherImpl dispatcher, final InetSocketAddress remoteAddress,
        final BGPPeerRegistry listener, final InetSocketAddress localAddress, final BGPHandlerFactory hf) throws InterruptedException {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(listener);

        final Bootstrap bootstrap = dispatcher.createClientBootStrap();
        bootstrap.localAddress(localAddress);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                ch.pipeline().addLast(hf.getDecoders());
                ch.pipeline().addLast("negotiator", snf.getSessionNegotiator(ch, new DefaultPromise<BGPSessionImpl>(ch.eventLoop())));
                ch.pipeline().addLast(hf.getEncoders());
            }
        });
        return bootstrap.connect(remoteAddress).sync();
    }

    private static void configurePeer(final String localAddress, final RIBImpl ribImpl, final BgpParameters bgpParameters, final PeerRole peerRole) {
        final InetAddress inetAddress = InetAddresses.forString(localAddress);
        final BGPPeer bgpPeer = new BGPPeer(inetAddress.getHostAddress(), ribImpl, peerRole);
        StrictBGPPeerRegistry.GLOBAL.addPeer(new IpAddress(new Ipv4Address(inetAddress.getHostAddress())), bgpPeer,
                new BGPSessionPreferences(AS_NUMBER, HOLDTIMER, new Ipv4Address(RIB_ID),
                        AS_NUMBER, Lists.newArrayList(bgpParameters)));
    }

    private static Channel connectPeer(final String localAddress, final RIBImpl ribImpl, final BgpParameters bgpParameters,
            final BGPDispatcherImpl dispatcherImpl, final BGPHandlerFactory hf, final BGPSessionListener sessionListsner) throws InterruptedException {
        final BGPPeerRegistry peerRegistry = new StrictBGPPeerRegistry();
        peerRegistry.addPeer(new IpAddress(new Ipv4Address(RIB_ID)), sessionListsner,
                new BGPSessionPreferences(AS_NUMBER, HOLDTIMER, new Ipv4Address(localAddress),
                        AS_NUMBER, Lists.newArrayList(bgpParameters)));
        final ChannelFuture createClient = createClient(dispatcherImpl, new InetSocketAddress(RIB_ID, PORT),
                peerRegistry, new InetSocketAddress(localAddress, PORT), hf);
        Thread.sleep(1000);
        return createClient.channel();
    }

    private static BgpParameters createParameter(final boolean addPath) {
        final OptionalCapabilities mp = new OptionalCapabilitiesBuilder().setCParameters(
                new CParametersBuilder().addAugmentation(CParameters1.class,
                        new CParameters1Builder().setMultiprotocolCapability(
                                new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
                                .build()).build()).build()).build();
        final List<OptionalCapabilities> capabilities = Lists.newArrayList(mp);
        if (addPath) {
            final OptionalCapabilities addPathCapa = new OptionalCapabilitiesBuilder().setCParameters(
                    new CParametersBuilder().addAugmentation(CParameters1.class,
                            new CParameters1Builder().setAddPathCapability(
                                    new AddPathCapabilityBuilder().setAddressFamilies(Lists.newArrayList(
                                            new AddressFamiliesBuilder()
                                            .setAfi(Ipv4AddressFamily.class)
                                            .setSafi(UnicastSubsequentAddressFamily.class)
                                            .setSendReceive(SendReceive.Both)
                                            .build()))
                                    .build()).build()).build()).build();
            capabilities.add(addPathCapa);
        }
        return new BgpParametersBuilder().setOptionalCapabilities(capabilities).build();
    }

    private static Update createSimpleUpdate(final String prefix, final String nextHop) {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        attBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        attBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build());
        attBuilder.addAugmentation(Attributes1.class,
                new Attributes1Builder().setMpReachNlri(
                        new MpReachNlriBuilder()
                        .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address(nextHop)).build()).build())
                        .setAfi(Ipv4AddressFamily.class)
                        .setSafi(UnicastSubsequentAddressFamily.class)
                        .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                                new DestinationIpv4CaseBuilder().setDestinationIpv4(
                                        new DestinationIpv4Builder().setIpv4Prefixes(Collections.singletonList(
                                                new Ipv4PrefixesBuilder().setPrefix(new Ipv4Prefix(prefix)).build())).build())
                                        .build()).build())
                        .build()).build());
        final Update update = new UpdateBuilder()
            .setAttributes(attBuilder.build()).build();
        return update;
    }

    private static final class HexDumpCollector extends SimpleChannelInboundHandler<ByteBuf> {

        private final List<String> messages = Lists.newArrayList();

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception {
            this.messages.add(ByteBufUtil.hexDump(msg));
        }

        public List<String> getReceivedMessages() {
            return ImmutableList.copyOf(messages);
        }
    }

}
