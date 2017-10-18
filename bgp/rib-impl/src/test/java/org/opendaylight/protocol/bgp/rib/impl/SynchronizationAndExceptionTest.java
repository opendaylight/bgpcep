/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class SynchronizationAndExceptionTest extends AbstractAddPathTest {
    private static final int HOLD_TIMER = 3;
    private static final AsNumber AS_NUMBER = new AsNumber(30L);
    private static final Ipv4Address BGP_ID = new Ipv4Address("1.1.1.2");
    private static final String LOCAL_IP = "1.1.1.4";
    private static final int LOCAL_PORT = 12345;
    private static final String RIB_ID = "1.1.1.2";
    private static final YangInstanceIdentifier PEER_PATH = YangInstanceIdentifier.builder()
            .node(BgpRib.QNAME).node(Rib.QNAME)
            .nodeWithKey(Rib.QNAME, QName.create(Rib.QNAME, "id").intern(), RIB_ID)
            .node(Peer.QNAME).nodeWithKey(Peer.QNAME, AdjRibInWriter.PEER_ID_QNAME, "bgp://1.1.1.2").build();
    private static final YangInstanceIdentifier TABLE_PATH = PEER_PATH.node(AdjRibIn.QNAME).node(Tables.QNAME)
            .node(RibSupportUtils.toYangTablesKey(new TablesKey(Ipv4AddressFamily.class,
                    UnicastSubsequentAddressFamily.class))).node(Attributes.QNAME)
            .node(QName.create(Attributes.QNAME, "uptodate"));
    private final IpAddress neighbor = new IpAddress(new Ipv4Address(LOCAL_IP));
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
    private DOMDataWriteTransaction tx;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        new EmbeddedChannel();
        final List<BgpParameters> tlvs = Lists.newArrayList();
        this.classicOpen = new OpenBuilder().setMyAsNumber(AS_NUMBER.getValue().intValue()).setHoldTimer(HOLD_TIMER)
                .setVersion(new ProtocolVersion((short) 4)).setBgpParameters(tlvs).setBgpIdentifier(BGP_ID).build();

        final List<OptionalCapabilities> capa = Lists.newArrayList();
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder()
                .addAugmentation(CParameters1.class,
                        new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                                .setAfi(this.ipv4tt.getAfi()).setSafi(this.ipv4tt.getSafi()).build())
                                .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build()).build())
                .setAs4BytesCapability(new As4BytesCapabilityBuilder()
                        .setAsNumber(AS_NUMBER).build()).build()).build());
        capa.add(new OptionalCapabilitiesBuilder()
                .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capa).build());

        doReturn(null).when(mock(ChannelFuture.class)).addListener(any());
        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();
        doReturn(true).when(this.speakerListener).isActive();
        doAnswer(invocation -> {
            final Runnable command = (Runnable) invocation.getArguments()[0];
            final long delay = (long) invocation.getArguments()[1];
            final TimeUnit unit = (TimeUnit) invocation.getArguments()[2];
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
                any(String.class), any(ChannelHandler.class));
        doReturn(null).when(this.pipeline).replace(Matchers.<Class<ChannelHandler>>any(),
                any(String.class), any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addLast(any(ChannelHandler.class));
        final ChannelFuture futureChannel = mock(ChannelFuture.class);
        doReturn(null).when(futureChannel).addListener(any());
        doReturn(futureChannel).when(this.speakerListener).close();
        doReturn(futureChannel).when(this.speakerListener).writeAndFlush(any(Notify.class));
        doReturn(this.domChain).when(this.domBroker).createTransactionChain(any());
        doReturn(this.tx).when(this.domChain).newWriteOnlyTransaction();
        final DOMDataTreeChangeService dOMDataTreeChangeService = mock(DOMDataTreeChangeService.class);
        final ListenerRegistration<?> listener = mock(ListenerRegistration.class);
        doReturn(listener).when(dOMDataTreeChangeService).registerDataTreeChangeListener(any(), any());
        doNothing().when(listener).close();

        doReturn(Collections.singletonMap(DOMDataTreeChangeService.class, dOMDataTreeChangeService))
                .when(this.domBroker).getSupportedExtensions();
        doNothing().when(this.tx).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(this.tx).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(this.tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        final CheckedFuture<?, ?> future = mock(CheckedFuture.class);
        doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(future).addListener(any(Runnable.class), any(Executor.class));
        doReturn(future).when(this.tx).submit();
        doReturn(mock(Optional.class)).when(future).checkedGet();
    }

    @Test
    public void testHandleMessageAfterException() throws InterruptedException {
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(TABLES_KEY,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy());
        final RIBImpl ribImpl = new RIBImpl(this.clusterSingletonServiceProvider, new RibId(RIB_ID), AS_NUMBER,
                new BgpId(RIB_ID), null, this.ribExtension, this.serverDispatcher,
                this.mappingService.getCodecFactory(), this.domBroker, ImmutableList.of(this.ipv4tt),
                pathTables, this.ribExtension.getClassLoadingStrategy());
        ribImpl.instantiateServiceInstance();
        ribImpl.onGlobalContextUpdated(this.schemaContext);

        final BGPPeer bgpPeer = new BGPPeer(neighbor, ribImpl, PeerRole.Ibgp, null, AFI_SAFIS_ADVERTIZED,
                Collections.emptySet());
        bgpPeer.instantiateServiceInstance();
        final BGPSessionImpl bgpSession = new BGPSessionImpl(bgpPeer, this.speakerListener, this.classicOpen,
                this.classicOpen.getHoldTimer(), null);
        bgpSession.setChannelExtMsgCoder(this.classicOpen);
        bgpPeer.onSessionUp(bgpSession);

        final Nlri n1 = new NlriBuilder().setPrefix(new Ipv4Prefix("8.0.1.0/28")).build();
        final Nlri n2 = new NlriBuilder().setPrefix( new Ipv4Prefix("127.0.0.1/32")).build();
        final Nlri n3 = new NlriBuilder().setPrefix( new Ipv4Prefix("2.2.2.2/24")).build();
        final List<Nlri> nlris = Lists.newArrayList(n1, n2, n3);
        final UpdateBuilder wrongMessage = new UpdateBuilder();
        wrongMessage.setNlri(nlris);
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final CNextHop nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4Address("127.0.0.1")).build()).build();
        final AttributesBuilder ab = new AttributesBuilder();
        wrongMessage.setAttributes(ab.setOrigin(origin).setAsPath(asPath).setCNextHop(nextHop).build());

        final UpdateBuilder correct = new UpdateBuilder(wrongMessage.build());
        correct.setAttributes(ab.setLocalPref(new LocalPrefBuilder().setPref((long) 100).build()).build());

        bgpSession.handleMessage(correct.build());
        verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(wrongMessage.build());
        verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(new UpdateBuilder().build());
        verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        verify(this.tx).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(PEER_PATH));
        verify(this.tx, times(0)).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(TABLE_PATH),
                eq(ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE)));
    }

    @Test
    public void testUseCase1() throws InterruptedException {
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(TABLES_KEY,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy());
        final RIBImpl ribImpl = new RIBImpl(this.clusterSingletonServiceProvider, new RibId(RIB_ID), AS_NUMBER,
                new BgpId(RIB_ID), null, this.ribExtension, this.serverDispatcher,
                this.mappingService.getCodecFactory(), this.domBroker, ImmutableList.of(this.ipv4tt), pathTables,
                this.ribExtension.getClassLoadingStrategy());
        ribImpl.instantiateServiceInstance();
        ribImpl.onGlobalContextUpdated(this.schemaContext);

        final BGPPeer bgpPeer = new BGPPeer(neighbor, ribImpl, PeerRole.Ibgp, null, AFI_SAFIS_ADVERTIZED,
                Collections.emptySet());
        bgpPeer.instantiateServiceInstance();
        final BGPSessionImpl bgpSession = new BGPSessionImpl(bgpPeer, this.speakerListener, this.classicOpen,
                this.classicOpen.getHoldTimer(), null);
        bgpSession.setChannelExtMsgCoder(this.classicOpen);
        bgpPeer.onSessionUp(bgpSession);

        final Nlri n1 = new NlriBuilder().setPrefix(new Ipv4Prefix("8.0.1.0/28")).build();
        final Nlri n2 = new NlriBuilder().setPrefix( new Ipv4Prefix("127.0.0.1/32")).build();
        final Nlri n3 = new NlriBuilder().setPrefix( new Ipv4Prefix("2.2.2.2/24")).build();
        final List<Nlri> nlris = Lists.newArrayList(n1, n2, n3);
        final UpdateBuilder wrongMessage = new UpdateBuilder();
        wrongMessage.setNlri(nlris);
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final CNextHop nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4Address("127.0.0.1")).build()).build();
        final AttributesBuilder ab = new AttributesBuilder();
        wrongMessage.setAttributes(ab.setOrigin(origin).setAsPath(asPath).setCNextHop(nextHop).build());

        final UpdateBuilder correct = new UpdateBuilder(wrongMessage.build());
        correct.setAttributes(ab.setLocalPref(new LocalPrefBuilder().setPref((long) 100).build()).build());

        bgpSession.handleMessage(correct.build());
        verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(new UpdateBuilder().build());
        verify(this.tx, times(5)).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        verify(this.tx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(TABLE_PATH),
                eq(ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE)));
        verify(this.tx, times(0)).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(PEER_PATH));
    }
}
