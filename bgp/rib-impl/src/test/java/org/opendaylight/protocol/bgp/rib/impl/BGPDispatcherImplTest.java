/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class BGPDispatcherImplTest {

    private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.10.0", 1790);
    private static final InetSocketAddress CLIENT_ADDRESS = new InetSocketAddress("127.0.11.0", 1791);
    private static final InetSocketAddress CLIENT_ADDRESS2 = new InetSocketAddress("127.0.12.0", 1792);
    private static final AsNumber AS_NUMBER = new AsNumber(30L);
    private static final int TIMEOUT = 5000;

    private final BgpTableType ipv4tt = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private BGPDispatcherImpl dispatcher;
    private TestClientDispatcher clientDispatcher;

    private BGPPeerRegistry registry;

    private Channel channel;

    @Before
    public void setUp() throws BGPDocumentedException {
        final EventLoopGroup group = new NioEventLoopGroup();
        this.registry = new StrictBGPPeerRegistry();
        this.registry.addPeer(new IpAddress(new Ipv4Address(CLIENT_ADDRESS.getAddress().getHostAddress())),
                new SimpleSessionListener(), createPreferences(CLIENT_ADDRESS));
        this.registry.addPeer(new IpAddress(new Ipv4Address(ADDRESS.getAddress().getHostAddress())),
                new SimpleSessionListener(), createPreferences(ADDRESS));
        this.dispatcher = new BGPDispatcherImpl(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry(), group, group);
        this.clientDispatcher = new TestClientDispatcher(group, group, ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry(),
                CLIENT_ADDRESS);

        final ChannelFuture future = this.dispatcher.createServer(this.registry, ADDRESS);
        future.addListener(new GenericFutureListener<Future<Void>>() {
            @Override
            public void operationComplete(final Future<Void> future) {
                if(!future.isSuccess()) {
                    Assert.fail("Failed to create server.");
                }
            }
        });
        this.channel = future.channel();
    }

    @Test
    public void testCreateClient() throws InterruptedException, ExecutionException {
        final BGPSessionImpl session = this.clientDispatcher.createClient(ADDRESS, this.registry,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, TIMEOUT), Optional.<InetSocketAddress>absent()).get();
        Assert.assertEquals(BGPSessionImpl.State.UP, session.getState());
        Assert.assertEquals(AS_NUMBER, session.getAsNumber());
        Assert.assertEquals(Sets.newHashSet(this.ipv4tt), session.getAdvertisedTableTypes());
        session.close();
    }

    @After
    public void tearDown() throws Exception {
        this.channel.close().get();
        this.dispatcher.close();
        this.registry.close();
    }

    @Test
    public void testCreateReconnectingClient() throws InterruptedException, ExecutionException {
        final SimpleSessionListener listener = new SimpleSessionListener();
        this.registry.addPeer(new IpAddress(new Ipv4Address(CLIENT_ADDRESS2.getAddress().getHostAddress())), listener, createPreferences(CLIENT_ADDRESS2));
        final Future<Void> cf = this.clientDispatcher.createReconnectingClient(CLIENT_ADDRESS2, this.registry,
                new ReconnectStrategyFctImpl(), Optional.<InetSocketAddress>absent());
        final Channel channel2 = this.dispatcher.createServer(this.registry, CLIENT_ADDRESS2).channel();
        Thread.sleep(1000);
        Assert.assertTrue(listener.up);
        Assert.assertTrue(channel2.isActive());
        cf.cancel(true);
        listener.releaseConnection();
    }

    private static final class ReconnectStrategyFctImpl implements ReconnectStrategyFactory {
        @Override
        public ReconnectStrategy createReconnectStrategy() {
            return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, TIMEOUT);
        }

    }

    private BGPSessionPreferences createPreferences(final InetSocketAddress socketAddress) {
        final List<BgpParameters> tlvs = Lists.newArrayList();
        final List<OptionalCapabilities> capas = Lists.newArrayList();
        capas.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(
            CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build()).build())
            .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(30L)).build()).build()).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        return new BGPSessionPreferences(AS_NUMBER, (short) 4, new Ipv4Address(socketAddress.getAddress().getHostAddress()), AS_NUMBER, tlvs);
    }

}
