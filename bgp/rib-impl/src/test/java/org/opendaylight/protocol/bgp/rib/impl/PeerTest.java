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

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class PeerTest extends AbstractRIBTestSetup {

    private final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    ApplicationPeer peer;

    BGPSessionImpl session;

    List<YangInstanceIdentifier> routes;

    private BGPPeer classic;

    @Mock
    Channel channel;

    @Mock
    ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.routes = new ArrayList<>();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final NormalizedNode<?,?> node = (NormalizedNode<?,?>)args[2];
                if (node.getNodeType().equals(Ipv4Route.QNAME) || node.getNodeType().equals(IPv4RIBSupport.PREFIX_QNAME)) {
                    PeerTest.this.routes.add((YangInstanceIdentifier) args[1]);
                }
                return args[1];
            }
        }).when(getTransaction()).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                PeerTest.this.routes.remove(args[1]);
                return args[1];
            }
        }).when(getTransaction()).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class));
    }

    @Test
    public void testAppPeer() {
        this.peer = new ApplicationPeer(new ApplicationRibId("t"), new Ipv4Address("127.0.0.1"), getRib());
        this.peer.onDataTreeChanged(appPeerInput(new Ipv4Prefix("127.0.0.2/32")));
        assertEquals(1, this.routes.size());
    }

    @Test
    public void testClassicPeer() throws Exception {
        this.classic = new BGPPeer("testPeer", getRib());
        Mockito.doReturn(null).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        Mockito.doReturn(this.eventLoop).when(this.channel).eventLoop();
        Mockito.doReturn(Boolean.TRUE).when(this.channel).isWritable();
        Mockito.doReturn(null).when(this.channel).close();
        Mockito.doReturn(this.pipeline).when(this.channel).pipeline();
        Mockito.doReturn(this.pipeline).when(this.pipeline).addLast(Mockito.any(ChannelHandler.class));
        Mockito.doReturn(new DefaultChannelPromise(this.channel)).when(this.channel).writeAndFlush(any(Notification.class));

        Mockito.doReturn(new InetSocketAddress("localhost", 12345)).when(this.channel).remoteAddress();
        Mockito.doReturn(new InetSocketAddress("localhost", 12345)).when(this.channel).localAddress();
        final List<BgpParameters> params = Lists.newArrayList(new BgpParametersBuilder().setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder().setCParameters(new MultiprotocolCaseBuilder()
            .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build())).build());
        this.session = new BGPSessionImpl(this.classic, this.channel, new OpenBuilder().setBgpIdentifier(new Ipv4Address("1.1.1.1")).setHoldTimer(50).setMyAsNumber(72).setBgpParameters(params).build(), 30, null);
        assertEquals("testPeer", this.classic.getName());
        this.classic.onSessionUp(this.session);
        Assert.assertArrayEquals(new byte[] {1, 1, 1, 1}, this.classic.getRawIdentifier());
        assertEquals("BGPPeer{name=testPeer, tables=[TablesKey [_afi=class org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily, _safi=class org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily]]}", this.classic.toString());
        final List<Ipv4Prefix> prefs = Lists.newArrayList(new Ipv4Prefix("127.0.0.1/32"), new Ipv4Prefix("2.2.2.2/24"));
        final UpdateBuilder ub = new UpdateBuilder();
        ub.setNlri(new NlriBuilder().setNlri(prefs).build());
        ub.setAttributes(new AttributesBuilder().build());
        this.classic.onMessage(this.session, ub.build());
        assertEquals(2, this.routes.size());

        //create new peer so that it gets advertized routes from RIB
        try (final BGPPeer testingPeer = new BGPPeer("testingPeer", getRib())) {
            testingPeer.onSessionUp(this.session);
            assertEquals(2, this.routes.size());
            assertEquals(1, testingPeer.getBgpPeerState().getSessionEstablishedCount().intValue());
            assertEquals(1, testingPeer.getBgpPeerState().getRouteTable().size());
            assertNotNull(testingPeer.getBgpSessionState());
        }

        ub.setNlri(null);
        ub.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(prefs).build());
        this.classic.onMessage(this.session, ub.build());
        assertEquals(0, this.routes.size());
        this.classic.onMessage(this.session, new KeepaliveBuilder().build());
        this.classic.onMessage(this.session, new UpdateBuilder().setAttributes(
            new AttributesBuilder().addAugmentation(
                    Attributes2.class,
                    new Attributes2Builder().setMpUnreachNlri(
                            new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build()).build());
        this.classic.releaseConnection();
    }
}
