/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.LoggerFactory;

public class AbstractBGPDispatcherTest {
    protected static final AsNumber AS_NUMBER = new AsNumber(Uint32.valueOf(30));
    static final int RETRY_TIMER = 1;
    protected static final BgpTableType IPV_4_TT = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final short HOLD_TIMER = 30;
    protected BGPDispatcherImpl clientDispatcher;
    protected StrictBGPPeerRegistry registry;
    protected SimpleSessionListener clientListener;
    protected BGPDispatcherImpl serverDispatcher;
    protected SimpleSessionListener serverListener;
    protected InetSocketAddress clientAddress;
    private EventLoopGroup boss;
    private EventLoopGroup worker;

    @Before
    public void setUp() {
        if (!Epoll.isAvailable()) {
            boss = new NioEventLoopGroup();
            worker = new NioEventLoopGroup();
        }
        registry = new StrictBGPPeerRegistry();
        clientListener = new SimpleSessionListener();
        serverListener = new SimpleSessionListener();
        final BGPExtensionConsumerContext ctx = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst()
            .orElseThrow();
        serverDispatcher = new BGPDispatcherImpl(ctx, boss, worker, registry);

        clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final IpAddressNoZone clientPeerIp = new IpAddressNoZone(new Ipv4AddressNoZone(
            clientAddress.getAddress().getHostAddress()));
        registry.addPeer(clientPeerIp, clientListener, createPreferences(clientAddress));
        clientDispatcher = new BGPDispatcherImpl(ctx, boss, worker, registry);
    }

    @After
    public void tearDown() throws Exception {
        serverDispatcher.close();
        registry.close();
        if (!Epoll.isAvailable()) {
            worker.shutdownGracefully(0, 0, TimeUnit.SECONDS);
            boss.shutdownGracefully(0, 0, TimeUnit.SECONDS);
        }
    }

    protected BGPSessionPreferences createPreferences(final InetSocketAddress socketAddress) {
        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> capas = new ArrayList<>();
        capas.add(new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                        .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                                .setAfi(IPV_4_TT.getAfi())
                                .setSafi(IPV_4_TT.getSafi())
                                .build())
                        .build())
                .setAs4BytesCapability(new As4BytesCapabilityBuilder()
                    .setAsNumber(new AsNumber(Uint32.valueOf(30)))
                    .build())
                .build())
            .build());
        capas.add(new OptionalCapabilitiesBuilder()
                .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        final BgpId bgpId = new BgpId(IetfInetUtil.INSTANCE.ipv4AddressFor(socketAddress.getAddress()));
        return new BGPSessionPreferences(AS_NUMBER, HOLD_TIMER, bgpId, AS_NUMBER, tlvs);
    }

    Channel createServer(final InetSocketAddress serverAddress) {
        registry.addPeer(new IpAddressNoZone(new Ipv4AddressNoZone(serverAddress.getAddress().getHostAddress())),
                serverListener, createPreferences(serverAddress));
        LoggerFactory.getLogger(AbstractBGPDispatcherTest.class).info("createServer");
        final ChannelFuture future = serverDispatcher.createServer(serverAddress);
        future.addListener(future1 -> Preconditions.checkArgument(future1.isSuccess(),
            "Unable to start bgp server on %s", future1.cause()));
        waitFutureSuccess(future);
        return future.channel();
    }
}
