/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.Set;
import org.junit.Before;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeerTrackerImpl;
import org.opendaylight.protocol.bgp.rib.impl.DefaultRibPoliciesMockTest;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class AbstractConfig extends DefaultRibPoliciesMockTest {
    protected static final AsNumber AS = new AsNumber(Uint32.valueOf(72));
    protected static final AsNumber LOCAL_AS = new AsNumber(Uint32.valueOf(73));
    protected static final RibId RIB_ID = new RibId("test");
    static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    @Mock
    protected RIB rib;
    @Mock
    protected BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Mock
    protected DOMTransactionChain domTx;
    @Mock
    protected BGPDispatcher dispatcher;
    @Mock
    protected BGPPeerRegistry bgpPeerRegistry;
    @Mock
    protected ListenerRegistration<?> listener;
    @Mock
    protected Future<?> future;
    @Mock
    protected DOMDataTreeWriteTransaction domDW;
    @Mock
    protected PeerGroupConfigLoader peerGroupLoader;
    @Mock
    private DOMDataTreeChangeService dataTreeChangeService;
    private final BGPPeerTracker peerTracker = new BGPPeerTrackerImpl();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        doReturn(InstanceIdentifier.create(BgpRib.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight
                .params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib.class, new RibKey(RIB_ID))).when(rib)
                .getInstanceIdentifier();
        doReturn(domTx).when(rib).createPeerDOMChain(any(DOMTransactionChainListener.class));

        doReturn(AS).when(rib).getLocalAs();
        doReturn(mock(RIBSupportContextRegistry.class)).when(rib).getRibSupportContext();
        doReturn(Set.of()).when(rib).getLocalTablesKeys();
        doNothing().when(domTx).close();
        doReturn(domDW).when(domTx).newWriteOnlyTransaction();
        doNothing().when(domDW).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(MapEntryNode.class));
        doNothing().when(domDW).delete(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class));
        doNothing().when(domDW).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doReturn(CommitInfo.emptyFluentFuture()).when(domDW).commit();

        doReturn(YangInstanceIdentifier.of(Rib.QNAME)).when(rib).getYangRibId();
        doReturn(dataTreeChangeService).when(rib).getService();
        doReturn(listener).when(dataTreeChangeService).registerDataTreeChangeListener(any(), any());
        doReturn(new BgpId("127.0.0.1")).when(rib).getBgpIdentifier();
        doReturn(true).when(future).cancel(true);
        doReturn(future).when(dispatcher).createReconnectingClient(any(InetSocketAddress.class),
                any(), anyInt(), any(KeyMapping.class));
        doReturn(dispatcher).when(rib).getDispatcher();

        doReturn(new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE))
                .when(tableTypeRegistry).getTableType(any());
        doReturn(TABLES_KEY).when(tableTypeRegistry).getTableKey(any());
        doReturn(Set.of(new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)))
                .when(rib).getLocalTables();

        doNothing().when(bgpPeerRegistry).addPeer(any(IpAddressNoZone.class),
                any(BGPSessionListener.class), any(BGPSessionPreferences.class));
        doNothing().when(bgpPeerRegistry).removePeer(any(IpAddressNoZone.class));
        doReturn("registry").when(bgpPeerRegistry).toString();
        doNothing().when(listener).close();
        doReturn(bgpPeerRegistry).when(dispatcher).getBGPPeerRegistry();
        doReturn(peerTracker).when(rib).getPeerTracker();
        doReturn(policies).when(rib).getRibPolicies();
        doReturn(null).when(peerGroupLoader)
                .getPeerGroup(any(InstanceIdentifier.class), any(String.class));
    }
}