/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class BGPDispatcherImplTest {

    private static final Ipv4Address IPV4 = new Ipv4Address("127.0.10.0");
    private static final InetSocketAddress ADDRESS = new InetSocketAddress(IPV4.getValue(), 1790);
    private static final AsNumber AS_NUMBER = new AsNumber(30L);
    private static final int TIMEOUT = 5000;

    private final BgpTableType ipv4tt = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private BGPDispatcherImpl dispatcher;

    @Mock
    private BGPPeerRegistry registry;

    private Channel channel;

    private SimpleSessionListener sessionListener;

    @Before
    public void setUp() throws BGPDocumentedException {
        MockitoAnnotations.initMocks(this);
        this.sessionListener = new SimpleSessionListener();
        final EventLoopGroup group = new NioEventLoopGroup();

        final List<BgpParameters> tlvs = Lists.newArrayList();
        tlvs.add(new BgpParametersBuilder().setCParameters(
            new MultiprotocolCaseBuilder().setMultiprotocolCapability(
                new MultiprotocolCapabilityBuilder().setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build()).build()).build());
        tlvs.add(new BgpParametersBuilder().setCParameters(new As4BytesCaseBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(
                new AsNumber(30L)).build()).build()).build());
        final BGPSessionPreferences prefs = new BGPSessionPreferences(AS_NUMBER, (short) 90, new Ipv4Address(ADDRESS.getAddress().getHostAddress()), tlvs);
        Mockito.doReturn(true).when(this.registry).isPeerConfigured(Mockito.any(IpAddress.class));
        Mockito.doReturn(prefs).when(this.registry).getPeerPreferences(Mockito.any(IpAddress.class));
        Mockito.doReturn(this.sessionListener).when(this.registry).getPeer(Mockito.any(IpAddress.class), Mockito.any(Ipv4Address.class), Mockito.any(Ipv4Address.class));

        this.dispatcher = new BGPDispatcherImpl(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry(), group, group);

        final ChannelFuture future = dispatcher.createServer(this.registry, new InetSocketAddress("0.0.0.0", 1790), new BGPServerSessionValidator());
        future.addListener(new GenericFutureListener<Future<Void>>() {
            @Override
            public void operationComplete(Future<Void> future) {
                if(!future.isSuccess()) {
                    Assert.fail("Failed to create server.");
                }
            }
        });
        this.channel = future.channel();
    }
    @Test
    public void testCreateClient() throws InterruptedException, ExecutionException {
        final BGPSessionImpl session = this.dispatcher.createClient(ADDRESS, AS_NUMBER, this.registry, new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, TIMEOUT)).get();
        Assert.assertTrue(this.sessionListener.up);
        Assert.assertEquals(BGPSessionImpl.State.Up, session.getState());
        Assert.assertEquals(AS_NUMBER, session.getAsNumber());
        Assert.assertEquals(IPV4, session.getBgpId());
        Assert.assertEquals(Sets.newHashSet(this.ipv4tt), session.getAdvertisedTableTypes());
        session.close();
    }

    @After
    public void tearDown() {
        this.channel.close();
        this.dispatcher.close();
    }

    @Test
    public void testCreateReconnectingClient() throws InterruptedException, ExecutionException {
        final Future<Void> cf = this.dispatcher.createReconnectingClient(ADDRESS, AS_NUMBER, this.registry, new ReconnectStrategyFctImpl(), new ReconnectStrategyFctImpl());
        cf.await(500);
        Assert.assertTrue(this.sessionListener.up);
    }

    private static final class ReconnectStrategyFctImpl implements ReconnectStrategyFactory {
        @Override
        public ReconnectStrategy createReconnectStrategy() {
            return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, TIMEOUT);
        }

    }

}
