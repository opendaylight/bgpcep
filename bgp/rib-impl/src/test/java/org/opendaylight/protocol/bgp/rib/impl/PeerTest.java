/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;

public class PeerTest extends AbstractRIBTestSetup {

    private final IpAddressNoZone neighborAddress = new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.1"));
    private ApplicationPeer peer;
    private BGPSessionImpl session;
    private Map<YangInstanceIdentifier, NormalizedNode> routes;
    private BGPPeer classic;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        routes = new HashMap<>();
        overrideMockedBehaviour();
    }

    private void overrideMockedBehaviour() {
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            final NormalizedNode node = (NormalizedNode) args[2];
            final QName nodeType = node.getIdentifier().getNodeType();

            if (nodeType.equals(Ipv4Route.QNAME) || nodeType.equals(PREFIX_QNAME)) {
                routes.put((YangInstanceIdentifier) args[1], node);
            }
            return args[1];
        }).when(getTransaction()).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            routes.remove(args[1]);
            return args[1];
        }).when(getTransaction()).delete(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class));
    }

    @Test
    public void testAppPeer() {
        final Ipv4Prefix first = new Ipv4Prefix("127.0.0.2/32");
        final Ipv4Prefix second = new Ipv4Prefix("127.0.0.1/32");
        final Ipv4Prefix third = new Ipv4Prefix("127.0.0.3/32");
        peer = new ApplicationPeer(tableRegistry,
                new ApplicationRibId(neighborAddress.getIpv4AddressNoZone().getValue()),
                neighborAddress.getIpv4AddressNoZone(), getRib());
        peer.instantiateServiceInstance(null, null);
        final YangInstanceIdentifier base = getRib().getYangRibId().node(LocRib.QNAME)
                .node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(KEY));
        peer.onDataTreeChanged(ipv4Input(base, ModificationType.WRITE, first, second, third));
        assertEquals(3, routes.size());

        peer.onDataTreeChanged(ipv4Input(base, ModificationType.DELETE, third));
        assertEquals(2, routes.size());
    }

    @Test
    public void testClassicPeer() throws Exception {
        classic = AbstractAddPathTest.configurePeer(tableRegistry,
            neighborAddress.getIpv4AddressNoZone(), getRib(), null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
        classic.instantiateServiceInstance();
        mockSession();
        assertEquals(neighborAddress.getIpv4AddressNoZone().getValue(), classic.getName());
        classic.onSessionUp(session);
        assertEquals("""
        	BGPPeer{name=127.0.0.1, tables=[TablesKey{_afi=interface org.opendaylight.yang.gen.v1\
        	.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily,\
        	 _safi=interface org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types\
        	.rev200120.UnicastSubsequentAddressFamily}]}""",
                classic.toString());

        final Nlri n1 = new NlriBuilder().setPrefix(new Ipv4Prefix("8.0.1.0/28")).build();
        final Nlri n2 = new NlriBuilder().setPrefix(new Ipv4Prefix("127.0.0.1/32")).build();
        final Nlri n3 = new NlriBuilder().setPrefix(new Ipv4Prefix("2.2.2.2/24")).build();
        final List<Nlri> nlris = Lists.newArrayList(n1, n2, n3);
        final UpdateBuilder ub = new UpdateBuilder();
        ub.setNlri(nlris);
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final CNextHop nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4AddressNoZone("127.0.0.1")).build()).build();
        final AttributesBuilder ab = new AttributesBuilder();
        ub.setAttributes(ab.setOrigin(origin).setAsPath(asPath).setCNextHop(nextHop).build());
        try {
            classic.onMessage(session, ub.build());
            fail();
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.MANDATORY_ATTR_MISSING_MSG + "LOCAL_PREF", e.getMessage());
            assertEquals(BGPError.WELL_KNOWN_ATTR_MISSING.getCode(), e.getError().getCode());
            assertEquals(BGPError.WELL_KNOWN_ATTR_MISSING.getSubcode(), e.getError().getSubcode());
            assertArrayEquals(new byte[]{LocalPreferenceAttributeParser.TYPE}, e.getData());
        }
        assertEquals(0, routes.size());

        final LocalPref localPref = new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build();
        ub.setAttributes(ab.setLocalPref(localPref).build());
        classic.onMessage(session, ub.build());
        assertEquals(3, routes.size());

        //create new peer so that it gets advertized routes from RIB
        final BGPPeer testingPeer = AbstractAddPathTest.configurePeer(tableRegistry,
            neighborAddress.getIpv4AddressNoZone(), getRib(), null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
        testingPeer.instantiateServiceInstance();
        testingPeer.onSessionUp(session);
        assertEquals(3, routes.size());

        final Nlri n11 = new NlriBuilder().setPrefix(new Ipv4Prefix("8.0.1.0/28")).build();
        final Nlri n22 = new NlriBuilder().setPrefix(new Ipv4Prefix("8.0.1.16/28")).build();
        final List<Nlri> nlris2 = Lists.newArrayList(n11, n22);
        ub.setNlri(nlris2);
        final WithdrawnRoutes w1 = new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("8.0.1.0/28")).build();
        final WithdrawnRoutes w2 = new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("127.0.0.1/32")).build();
        final WithdrawnRoutes w3 = new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("2.2.2.2/24")).build();
        final List<WithdrawnRoutes> wrs = Lists.newArrayList(w1, w2, w3);
        ub.setWithdrawnRoutes(wrs);
        classic.onMessage(session, ub.build());
        assertEquals(2, routes.size());
        classic.onMessage(session, new KeepaliveBuilder().build());
        classic.onMessage(session, new UpdateBuilder()
            .setAttributes(new AttributesBuilder()
                .addAugmentation(new AttributesUnreachBuilder()
                    .setMpUnreachNlri(new MpUnreachNlriBuilder().setAfi(IPV4_AFI).setSafi(SAFI).build())
                    .build())
                .build())
            .build());
        classic.onMessage(session, new RouteRefreshBuilder().setAfi(IPV4_AFI).setSafi(SAFI).build());
        classic.onMessage(session, new RouteRefreshBuilder()
                .setAfi(Ipv6AddressFamily.VALUE)
                .setSafi(SAFI).build());
        assertEquals(2, routes.size());
        classic.releaseConnection();
    }

    private void mockSession() {
        final EventLoop eventLoop = mock(EventLoop.class);
        final Channel channel = mock(Channel.class);
        final ChannelPipeline pipeline = mock(ChannelPipeline.class);
        doReturn(null).when(eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn(eventLoop).when(channel).eventLoop();
        doReturn(Boolean.TRUE).when(channel).isWritable();
        doReturn(null).when(channel).close();
        doReturn(pipeline).when(channel).pipeline();
        doCallRealMethod().when(channel).toString();
        doReturn(pipeline).when(pipeline).addLast(any(ChannelHandler.class));
        doReturn(new DefaultChannelPromise(channel)).when(channel).writeAndFlush(any(Notification.class));
        doReturn(new InetSocketAddress("localhost", 12345)).when(channel).remoteAddress();
        doReturn(new InetSocketAddress("localhost", 12345)).when(channel).localAddress();
        final List<BgpParameters> params = Lists.newArrayList(new BgpParametersBuilder()
            .setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .addAugmentation(new CParameters1Builder()
                        .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                            .setAfi(Ipv4AddressFamily.VALUE)
                            .setSafi(UnicastSubsequentAddressFamily.VALUE)
                            .build())
                        .build())
                    .build())
                .build()))
            .build());
        final Open openObj = new OpenBuilder()
                .setBgpIdentifier(new Ipv4AddressNoZone("1.1.1.1"))
                .setHoldTimer(Uint16.valueOf(50))
                .setMyAsNumber(Uint16.valueOf(72))
                .setBgpParameters(params).build();
        session = new BGPSessionImpl(classic, channel, openObj, 30, null);
        session.setChannelExtMsgCoder(openObj);
    }
}
