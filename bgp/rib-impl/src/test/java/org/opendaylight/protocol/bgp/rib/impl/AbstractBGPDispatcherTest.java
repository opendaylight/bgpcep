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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.LoggerFactory;

public class AbstractBGPDispatcherTest {
    protected static final AsNumber AS_NUMBER = new AsNumber(Uint32.valueOf(30));
    static final int RETRY_TIMER = 1;
    protected static final BgpTableType IPV_4_TT = new BgpTableTypeImpl(Ipv4AddressFamily.class,
        UnicastSubsequentAddressFamily.class);
    private static final short HOLD_TIMER = 30;
    protected BGPDispatcherImpl clientDispatcher;
    protected BGPPeerRegistry registry;
    protected SimpleSessionListener clientListener;
    protected BGPDispatcherImpl serverDispatcher;
    protected SimpleSessionListener serverListener;
    protected InetSocketAddress clientAddress;
    private EventLoopGroup boss;
    private EventLoopGroup worker;

    @Before
    public void setUp() {
        if (!Epoll.isAvailable()) {
            this.boss = new NioEventLoopGroup();
            this.worker = new NioEventLoopGroup();
        }
        this.registry = new StrictBGPPeerRegistry();
        this.clientListener = new SimpleSessionListener();
        this.serverListener = new SimpleSessionListener();
        final BGPExtensionProviderContext ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        this.serverDispatcher = new BGPDispatcherImpl(ctx.getMessageRegistry(), this.boss, this.worker, this.registry);

        this.clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final IpAddress clientPeerIp = new IpAddress(new Ipv4Address(this.clientAddress.getAddress().getHostAddress()));
        this.registry.addPeer(clientPeerIp, this.clientListener, createPreferences(this.clientAddress));
        this.clientDispatcher = new BGPDispatcherImpl(ctx.getMessageRegistry(), this.boss, this.worker, this.registry);
    }

    @After
    public void tearDown() throws Exception {
        this.serverDispatcher.close();
        this.registry.close();
        if (!Epoll.isAvailable()) {
            this.worker.shutdownGracefully(0, 0, TimeUnit.SECONDS);
            this.boss.shutdownGracefully(0, 0, TimeUnit.SECONDS);
        }
    }

    protected BGPSessionPreferences createPreferences(final InetSocketAddress socketAddress) {
        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> capas = new ArrayList<>();
        capas.add(new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder()
                .addAugmentation(CParameters1.class, new CParameters1Builder()
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
        final BgpId bgpId = new BgpId(IetfInetUtil.INSTANCE.ipv4AddressNoZoneFor(socketAddress.getAddress()));
        return new BGPSessionPreferences(AS_NUMBER, HOLD_TIMER, bgpId, AS_NUMBER, tlvs);
    }

    Channel createServer(final InetSocketAddress serverAddress) {
        this.registry.addPeer(new IpAddress(new Ipv4Address(serverAddress.getAddress().getHostAddress())),
                this.serverListener, createPreferences(serverAddress));
        LoggerFactory.getLogger(AbstractBGPDispatcherTest.class).info("createServer");
        final ChannelFuture future = this.serverDispatcher.createServer(serverAddress);
        future.addListener(future1 -> Preconditions.checkArgument(future1.isSuccess(),
            "Unable to start bgp server on %s", future1.cause()));
        waitFutureSuccess(future);
        return future.channel();
    }
}
