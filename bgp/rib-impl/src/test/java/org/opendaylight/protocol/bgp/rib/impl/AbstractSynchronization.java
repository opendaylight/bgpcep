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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
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
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

abstract class AbstractSynchronization extends AbstractAddPathTest {
    protected static final String RIB_ID = "1.1.1.2";
    protected static final YangInstanceIdentifier PEER_PATH = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Rib.QNAME)
        .nodeWithKey(Rib.QNAME, QName.create(Rib.QNAME, "id").intern(), RIB_ID)
        .node(Peer.QNAME).nodeWithKey(Peer.QNAME, AdjRibInWriter.PEER_ID_QNAME, "bgp://1.1.1.2").build();
    protected static final YangInstanceIdentifier TABLE_PATH = PEER_PATH.node(AdjRibIn.QNAME).node(Tables.QNAME)
        .node(RibSupportUtils.toYangTablesKey(new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class))).node(Attributes.QNAME).node(QName.create(Attributes.QNAME, "uptodate"));
    protected static final AsNumber AS_NUMBER = new AsNumber(30L);
    protected static final BgpTableType BGP_TABLE_TYPE = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final int HOLD_TIMER = 180;
    private static final Ipv4Address BGP_ID = new Ipv4Address("1.1.1.2");
    private static final String LOCAL_IP = "1.1.1.4";
    private static final int LOCAL_PORT = 12345;

    protected Open classicOpen;
    @Mock
    protected Channel speakerListener;
    @Mock
    protected DOMDataBroker domBroker;
    @Mock
    protected DOMDataWriteTransaction tx;
    @Mock
    private EventLoop eventLoop;
    @Mock
    private ChannelPipeline pipeline;
    @Mock
    private DOMTransactionChain domChain;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        new EmbeddedChannel();
        final List<BgpParameters> tlvs = Lists.newArrayList();
        this.classicOpen = new OpenBuilder().setMyAsNumber(AS_NUMBER.getValue().intValue()).setHoldTimer(HOLD_TIMER)
            .setVersion(new ProtocolVersion((short) 4)).setBgpParameters(tlvs).setBgpIdentifier(BGP_ID).build();

        final List<OptionalCapabilities> capa = Lists.newArrayList();
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(BGP_TABLE_TYPE.getAfi()).setSafi(BGP_TABLE_TYPE.getSafi()).build())
                .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build()).build())
            .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(AS_NUMBER).build()).build()).build());
        capa.add(new OptionalCapabilitiesBuilder().setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(capa).build());

        doReturn(null).when(mock(ChannelFuture.class)).addListener(any());
        doReturn(this.eventLoop).when(this.speakerListener).eventLoop();
        doReturn(true).when(this.speakerListener).isActive();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Runnable command = (Runnable) invocation.getArguments()[0];
                final long delay = (long) invocation.getArguments()[1];
                final TimeUnit unit = (TimeUnit) invocation.getArguments()[2];
                GlobalEventExecutor.INSTANCE.schedule(command, delay, unit);
                return null;
            }
        }).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn("TestingChannel").when(this.speakerListener).toString();
        doReturn(true).when(this.speakerListener).isWritable();
        doReturn(new InetSocketAddress(InetAddress.getByName(BGP_ID.getValue()), 179)).when(this.speakerListener).remoteAddress();
        doReturn(new InetSocketAddress(InetAddress.getByName(LOCAL_IP), LOCAL_PORT)).when(this.speakerListener).localAddress();
        doReturn(this.pipeline).when(this.speakerListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
        doReturn(null).when(this.pipeline).replace(Matchers.<Class<ChannelHandler>>any(), any(String.class), any(ChannelHandler.class));
        doReturn(this.pipeline).when(this.pipeline).addLast(any(ChannelHandler.class));
        final ChannelFuture futureChannel = mock(ChannelFuture.class);
        doReturn(null).when(futureChannel).addListener(any());
        doReturn(futureChannel).when(this.speakerListener).close();
        doReturn(futureChannel).when(this.speakerListener).writeAndFlush(any(Notify.class));
        doReturn(this.domChain).when(this.domBroker).createTransactionChain(any());
        doReturn(this.tx).when(this.domChain).newWriteOnlyTransaction();
        final DOMDataTreeChangeService dOMDataTreeChangeService = mock(DOMDataTreeChangeService.class);
        final ListenerRegistration listener = mock(ListenerRegistration.class);
        Mockito.doReturn(listener).when(dOMDataTreeChangeService).registerDataTreeChangeListener(any(), any());
        Mockito.doNothing().when(listener).close();

        Mockito.doReturn(Collections.singletonMap(DOMDataTreeChangeService.class, dOMDataTreeChangeService))
            .when(this.domBroker).getSupportedExtensions();
        Mockito.doNothing().when(this.tx).merge(eq(LogicalDatastoreType.OPERATIONAL),
            any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        Mockito.doNothing().when(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL),
            any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        Mockito.doNothing().when(this.tx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class));
        final CheckedFuture future = mock(CheckedFuture.class);
        Mockito.doReturn(future).when(this.tx).submit();
        Mockito.doReturn(mock(Optional.class)).when(future).checkedGet();
    }

}
