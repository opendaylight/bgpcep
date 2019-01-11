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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.FluentFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.common.api.CommitInfo;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class AbstractConfig extends DefaultRibPoliciesMockTest {
    protected static final AsNumber AS = new AsNumber(72L);
    protected static final AsNumber LOCAL_AS = new AsNumber(73L);
    protected static final RibId RIB_ID = new RibId("test");
    static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
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
    protected DOMDataWriteTransaction domDW;
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
                .params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib.class, new RibKey(RIB_ID))).when(this.rib)
                .getInstanceIdentifier();
        doReturn(this.domTx).when(this.rib).createPeerDOMChain(any(TransactionChainListener.class));
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            return getDataBroker().createTransactionChain((TransactionChainListener) args[0]);
        }).when(this.rib).createPeerChain(any(TransactionChainListener.class));

        doReturn(getDataBroker()).when(this.rib).getDataBroker();
        doReturn(AS).when(this.rib).getLocalAs();
        doReturn(mock(RIBSupportContextRegistry.class)).when(this.rib).getRibSupportContext();
        doReturn(Collections.emptySet()).when(this.rib).getLocalTablesKeys();
        doNothing().when(this.domTx).close();
        doReturn(this.domDW).when(this.domTx).newWriteOnlyTransaction();
        doNothing().when(this.domDW).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(MapEntryNode.class));
        doNothing().when(this.domDW).delete(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class));
        doNothing().when(this.domDW).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        final FluentFuture<? extends CommitInfo> checkedFuture = mock(FluentFuture.class);
        doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(checkedFuture).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        doReturn(checkedFuture).when(this.domDW).commit();
        doReturn(null).when(checkedFuture).get();
        doReturn(true).when(checkedFuture).isDone();
        doReturn("checkedFuture").when(checkedFuture).toString();
        doReturn(YangInstanceIdentifier.of(Rib.QNAME)).when(this.rib).getYangRibId();
        doReturn(this.dataTreeChangeService).when(this.rib).getService();
        doReturn(this.listener).when(this.dataTreeChangeService).registerDataTreeChangeListener(any(), any());
        doReturn(new BgpId("127.0.0.1")).when(this.rib).getBgpIdentifier();
        doReturn(true).when(this.future).cancel(true);
        doReturn(this.future).when(this.dispatcher).createReconnectingClient(any(InetSocketAddress.class),
                any(), anyInt(), any(KeyMapping.class));
        doReturn(this.dispatcher).when(this.rib).getDispatcher();

        doReturn(java.util.Optional.of(new BgpTableTypeImpl(Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class)))
                .when(this.tableTypeRegistry).getTableType(any());
        doReturn(java.util.Optional.of(TABLES_KEY)).when(this.tableTypeRegistry).getTableKey(any());
        doReturn(Collections.singleton(new BgpTableTypeImpl(Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class)))
                .when(this.rib).getLocalTables();

        doNothing().when(this.bgpPeerRegistry).addPeer(any(IpAddress.class),
                any(BGPSessionListener.class), any(BGPSessionPreferences.class));
        doNothing().when(this.bgpPeerRegistry).removePeer(any(IpAddress.class));
        doReturn("registry").when(this.bgpPeerRegistry).toString();
        doNothing().when(this.listener).close();
        doReturn(this.bgpPeerRegistry).when(this.dispatcher).getBGPPeerRegistry();
        doReturn(this.peerTracker).when(this.rib).getPeerTracker();
        doReturn(this.policies).when(this.rib).getRibPolicies();
        doReturn(null).when(this.peerGroupLoader)
                .getPeerGroup(any(InstanceIdentifier.class), any(String.class));
    }
}