/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.BGPRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.RIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.spi.RIBQNames;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class SynchronizationAndExceptionTest extends AbstractAddPathTest {
    private static final int HOLD_TIMER = 3;
    private static final AsNumber AS_NUMBER = new AsNumber(Uint32.valueOf(30));
    private static final Ipv4AddressNoZone BGP_ID = new Ipv4AddressNoZone("1.1.1.2");
    private static final String LOCAL_IP = "1.1.1.4";
    private static final int LOCAL_PORT = 12345;
    private static final String RIB_ID = "1.1.1.2";
    private static final YangInstanceIdentifier PEER_PATH = YangInstanceIdentifier.builder()
            .node(BGPRIB_NID).node(RIB_NID)
            .nodeWithKey(Rib.QNAME, QName.create(Rib.QNAME, "id").intern(), RIB_ID)
            .node(PEER_NID).nodeWithKey(Peer.QNAME, RIBQNames.PEER_ID_QNAME, "bgp://1.1.1.2").build();
    private static final YangInstanceIdentifier TABLE_PATH = PEER_PATH.node(ADJRIBIN_NID).node(TABLES_NID)
            .node(RibSupportUtils.toYangTablesKey(new TablesKey(Ipv4AddressFamily.class,
                    UnicastSubsequentAddressFamily.class))).node(ATTRIBUTES_NID)
            .node(UPTODATE_NID);
    private final IpAddressNoZone neighbor = new IpAddressNoZone(new Ipv4AddressNoZone(LOCAL_IP));
    private final BgpTableType ipv4tt = new BgpTableTypeImpl(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private Open classicOpen;
    @Mock
    private EventLoop eventLoop;
    @Mock
    private ChannelPipeline pipeline;
    @Mock
    private Channel speakerListener;
    @Mock
    private DOMDataBroker domBroker;
    @Mock
    private DOMTransactionChain domChain;
    @Mock
    private DOMDataTreeWriteTransaction tx;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        final List<OptionalCapabilities> capa = new ArrayList<>();
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                        .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build())
                        .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().setRestartTime(Uint16.ZERO)
                                .build()).build())
                .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(AS_NUMBER).build()).build())
                .build());
        capa.add(new OptionalCapabilitiesBuilder()
                .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());

        this.classicOpen = new OpenBuilder()
                .setMyAsNumber(Uint16.valueOf(AS_NUMBER.getValue()))
                .setHoldTimer(Uint16.valueOf(HOLD_TIMER))
                .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
                .setBgpParameters(List.of(new BgpParametersBuilder()
                    .setOptionalCapabilities(capa)
                    .build()))
                .setBgpIdentifier(BGP_ID)
                .build();

        doReturn(null).when(mock(ChannelFuture.class)).addListener(any());
        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();
        doReturn(true).when(this.speakerListener).isActive();
        doAnswer(invocation -> {
            final Runnable command = invocation.getArgument(0);
            final long delay = (long) invocation.getArgument(1);
            final TimeUnit unit = invocation.getArgument(2);
            GlobalEventExecutor.INSTANCE.schedule(command, delay, unit);
            return null;
        }).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(true).when(this.speakerListener).isWritable();
        doReturn(new InetSocketAddress(InetAddress.getByName(BGP_ID.getValue()), 179))
                .when(this.speakerListener).remoteAddress();
        doReturn(new InetSocketAddress(InetAddress.getByName(LOCAL_IP), LOCAL_PORT))
                .when(this.speakerListener).localAddress();
        doReturn(this.pipeline).when(this.speakerListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class),
                any(String.class),
                any(ChannelHandler.class));
        doReturn(null).when(this.pipeline).replace(ArgumentMatchers.<Class<ChannelHandler>>any(),
                any(String.class),
                any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addLast(any(ChannelHandler.class));
        final ChannelFuture futureChannel = mock(ChannelFuture.class);
        doReturn(null).when(futureChannel).addListener(any());
        doReturn(futureChannel).when(this.speakerListener).close();
        doReturn(futureChannel).when(this.speakerListener).writeAndFlush(any(Notify.class));
        doReturn(this.domChain).when(this.domBroker).createMergingTransactionChain(any());
        doReturn(this.tx).when(this.domChain).newWriteOnlyTransaction();
        final DOMDataTreeChangeService dOMDataTreeChangeService = mock(DOMDataTreeChangeService.class);
        final ListenerRegistration<?> listener = mock(ListenerRegistration.class);
        doReturn(listener).when(dOMDataTreeChangeService).registerDataTreeChangeListener(any(), any());
        doNothing().when(listener).close();
        doNothing().when(this.domChain).close();

        doReturn(ImmutableClassToInstanceMap.of(DOMDataTreeChangeService.class, dOMDataTreeChangeService))
                .when(this.domBroker).getExtensions();
        doNothing().when(this.tx).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(this.tx).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(this.tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        doReturn(CommitInfo.emptyFluentFuture()).when(this.tx).commit();
    }

    @Test
    public void testHandleMessageAfterException() {
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(TABLES_KEY,
            BasePathSelectionModeFactory.createBestPathSelectionStrategy());
        final RIBImpl ribImpl = new RIBImpl(this.tableRegistry, new RibId(RIB_ID), AS_NUMBER,  new BgpId(RIB_ID),
                this.ribExtension,
                this.serverDispatcher, this.codecsRegistry, this.domBroker, getDataBroker(), this.policies,
                ImmutableList.of(this.ipv4tt), pathTables);
        ribImpl.instantiateServiceInstance();

        final BGPPeer bgpPeer = AbstractAddPathTest.configurePeer(this.tableRegistry, neighbor.getIpv4AddressNoZone(),
            ribImpl, null, PeerRole.Ibgp, this.serverRegistry, AFI_SAFIS_ADVERTIZED, Collections.emptySet());
        bgpPeer.instantiateServiceInstance();
        final BGPSessionImpl bgpSession = new BGPSessionImpl(bgpPeer, this.speakerListener, this.classicOpen,
                this.classicOpen.getHoldTimer().toJava(), null);
        bgpSession.setChannelExtMsgCoder(this.classicOpen);
        bgpPeer.onSessionUp(bgpSession);

        final Nlri n1 = new NlriBuilder().setPrefix(new Ipv4Prefix("8.0.1.0/28")).build();
        final Nlri n2 = new NlriBuilder().setPrefix(new Ipv4Prefix("127.0.0.1/32")).build();
        final Nlri n3 = new NlriBuilder().setPrefix(new Ipv4Prefix("2.2.2.2/24")).build();
        final List<Nlri> nlris = Lists.newArrayList(n1, n2, n3);
        final UpdateBuilder wrongMessage = new UpdateBuilder();
        wrongMessage.setNlri(nlris);
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final CNextHop nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4AddressNoZone("127.0.0.1")).build()).build();
        final AttributesBuilder ab = new AttributesBuilder();
        wrongMessage.setAttributes(ab.setOrigin(origin).setAsPath(asPath).setCNextHop(nextHop).build());

        final UpdateBuilder correct = new UpdateBuilder(wrongMessage.build());
        correct.setAttributes(ab.setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build()).build());

        bgpSession.handleMessage(correct.build());
        verify(this.tx, times(2)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(wrongMessage.build());
        verify(this.tx, times(2)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(new UpdateBuilder().build());
        verify(this.tx, times(2)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        verify(this.tx).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(PEER_PATH));
        verify(this.tx, times(0)).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(TABLE_PATH),
                eq(ImmutableNodes.leafNode(UPTODATE_NID, Boolean.TRUE)));
    }

    @Test
    public void testUseCase1() {
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(TABLES_KEY,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy());
        final RIBImpl ribImpl = new RIBImpl(this.tableRegistry, new RibId(RIB_ID), AS_NUMBER, new BgpId(RIB_ID),
                this.ribExtension,
                this.serverDispatcher, this.codecsRegistry, this.domBroker, getDataBroker(), this.policies,
                ImmutableList.of(this.ipv4tt), pathTables);
        ribImpl.instantiateServiceInstance();

        final BGPPeer bgpPeer = AbstractAddPathTest.configurePeer(this.tableRegistry, neighbor.getIpv4AddressNoZone(),
            ribImpl, null, PeerRole.Ibgp, this.serverRegistry, AFI_SAFIS_ADVERTIZED, Collections.emptySet());
        bgpPeer.instantiateServiceInstance();
        final BGPSessionImpl bgpSession = new BGPSessionImpl(bgpPeer, this.speakerListener, this.classicOpen,
                this.classicOpen.getHoldTimer().toJava(), null);
        bgpSession.setChannelExtMsgCoder(this.classicOpen);
        bgpPeer.onSessionUp(bgpSession);

        final Nlri n1 = new NlriBuilder().setPrefix(new Ipv4Prefix("8.0.1.0/28")).build();
        final Nlri n2 = new NlriBuilder().setPrefix(new Ipv4Prefix("127.0.0.1/32")).build();
        final Nlri n3 = new NlriBuilder().setPrefix(new Ipv4Prefix("2.2.2.2/24")).build();
        final List<Nlri> nlris = Lists.newArrayList(n1, n2, n3);
        final UpdateBuilder wrongMessage = new UpdateBuilder();
        wrongMessage.setNlri(nlris);
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final CNextHop nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4AddressNoZone("127.0.0.1")).build()).build();
        final AttributesBuilder ab = new AttributesBuilder();
        wrongMessage.setAttributes(ab.setOrigin(origin).setAsPath(asPath).setCNextHop(nextHop).build());

        final UpdateBuilder correct = new UpdateBuilder(wrongMessage.build());
        correct.setAttributes(ab.setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build()).build());

        bgpSession.handleMessage(correct.build());
        verify(this.tx, times(2)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(new UpdateBuilder().build());
        verify(this.tx, times(3)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        verify(this.tx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(TABLE_PATH),
                eq(ImmutableNodes.leafNode(UPTODATE_NID, Boolean.TRUE)));
        verify(this.tx, times(0)).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(PEER_PATH));
    }
}
