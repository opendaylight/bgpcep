/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.slf4j.LoggerFactory;

public class BGPDispatcherImplTest {
    private static final short HOLD_TIMER = 30;
    private static final AsNumber AS_NUMBER = new AsNumber(30L);
    private static final int RETRY_TIMER = 1;
    private static final BgpTableType IPV_4_TT = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private BGPDispatcherImpl serverDispatcher;
    private TestClientDispatcher clientDispatcher;
    private BGPPeerRegistry registry;
    private SimpleSessionListener clientListener;
    private SimpleSessionListener serverListener;
    private EventLoopGroup boss;
    private EventLoopGroup worker;

    @Before
    public void setUp() throws BGPDocumentedException {
        if (Epoll.isAvailable()) {
            this.boss = new EpollEventLoopGroup();
            this.worker = new EpollEventLoopGroup();
        } else {
            this.boss = new NioEventLoopGroup();
            this.worker = new NioEventLoopGroup();
        }
        this.registry = new StrictBGPPeerRegistry();
        this.clientListener = new SimpleSessionListener();
        final BGPExtensionProviderContext ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        this.serverDispatcher = new BGPDispatcherImpl(ctx.getMessageRegistry(), this.boss, this.worker);
        configureClient(ctx);
    }

    static <T extends Future> void waitFutureSuccess(final T future) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        future.addListener(future1 -> latch.countDown());
        Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.SECONDS);
    }


    public static void checkIdleState (final SimpleSessionListener listener){
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 10) {
            if (BGPSessionImpl.State.IDLE != listener.getState()){
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            } else {
                return;
            }
        }
        Assert.fail();
    }

    private void configureClient(final BGPExtensionProviderContext ctx) {
        final InetSocketAddress clientAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final IpAddress clientPeerIp = new IpAddress(new Ipv4Address(clientAddress.getAddress().getHostAddress()));
        this.registry.addPeer(clientPeerIp, this.clientListener, createPreferences(clientAddress));
        this.clientDispatcher = new TestClientDispatcher(this.boss, this.worker, ctx.getMessageRegistry(), clientAddress);
    }

    private Channel createServer(final InetSocketAddress serverAddress) throws InterruptedException {
        this.serverListener = new SimpleSessionListener();
        this.registry.addPeer(new IpAddress(new Ipv4Address(serverAddress.getAddress().getHostAddress())), this.serverListener, createPreferences(serverAddress));
        LoggerFactory.getLogger(BGPDispatcherImplTest.class).info("createServer");
        final ChannelFuture future = this.serverDispatcher.createServer(this.registry, serverAddress);
        future.addListener(new GenericFutureListener<Future<Void>>() {
            @Override
            public void operationComplete(final Future<Void> future) {
                Preconditions.checkArgument(future.isSuccess(), "Unable to start bgp server on %s", future.cause());
            }
        });
        waitFutureSuccess(future);
        return future.channel();
    }

    @After
    public void tearDown() throws Exception {
        this.serverDispatcher.close();
        this.registry.close();
        this.worker.shutdownGracefully().awaitUninterruptibly();
        this.boss.shutdownGracefully().awaitUninterruptibly();
    }

    @Test
    public void testCreateClient() throws InterruptedException, ExecutionException {
        final InetSocketAddress serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final Channel serverChannel = createServer(serverAddress);
        Thread.sleep(1000);
        final Future<BGPSessionImpl> futureClient = this.clientDispatcher.createClient(serverAddress, this.registry, 2, Optional.absent());
        waitFutureSuccess(futureClient);
        final BGPSessionImpl session = futureClient.get();
        Assert.assertEquals(BGPSessionImpl.State.UP, this.clientListener.getState());
        Assert.assertEquals(BGPSessionImpl.State.UP, this.serverListener.getState());
        Assert.assertEquals(AS_NUMBER, session.getAsNumber());
        Assert.assertEquals(Sets.newHashSet(IPV_4_TT), session.getAdvertisedTableTypes());
        Assert.assertTrue(serverChannel.isWritable());
        session.close();
        this.serverListener.releaseConnection();
        checkIdleState(this.clientListener);
        checkIdleState(this.serverListener);
    }

    @Test
    public void testCreateReconnectingClient() throws Exception {
        final InetSocketAddress serverAddress = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final Future<Void> future = this.clientDispatcher.createReconnectingClient(serverAddress, this.registry, RETRY_TIMER, Optional.absent());
        waitFutureSuccess(future);
        final Channel serverChannel = createServer(serverAddress);
        Assert.assertEquals(BGPSessionImpl.State.UP, this.serverListener.getState());
        Assert.assertTrue(serverChannel.isWritable());
        future.cancel(true);
        this.serverListener.releaseConnection();
        checkIdleState(this.serverListener);
    }

    private BGPSessionPreferences createPreferences(final InetSocketAddress socketAddress) {
        final List<BgpParameters> tlvs = Lists.newArrayList();
        final List<OptionalCapabilities> capas = Lists.newArrayList();
        capas.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(
            CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(IPV_4_TT.getAfi()).setSafi(IPV_4_TT.getSafi()).build()).build())
            .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(30L)).build())
            .build()).build());
        capas.add(new OptionalCapabilitiesBuilder().setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        final BgpId bgpId = new BgpId(new Ipv4Address(socketAddress.getAddress().getHostAddress()));
        return new BGPSessionPreferences(AS_NUMBER, HOLD_TIMER, bgpId, AS_NUMBER, tlvs, Optional.absent());
    }
}
