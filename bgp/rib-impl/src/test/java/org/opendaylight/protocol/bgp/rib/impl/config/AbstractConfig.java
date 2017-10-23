/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.CheckedFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.AbstractImportPolicy;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.osgi.framework.ServiceRegistration;

class AbstractConfig {
    static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    protected static final AsNumber AS = new AsNumber(72L);
    protected ClusterSingletonService singletonService;
    @Mock
    protected RIB rib;
    @Mock
    protected ClusterSingletonServiceRegistration singletonServiceRegistration;
    @Mock
    protected BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Mock
    protected BgpDeployer.WriteConfiguration configurationWriter;
    @Mock
    protected DOMTransactionChain domTx;
    @Mock
    protected BGPDispatcher dispatcher;
    @Mock
    protected ServiceRegistration<?> serviceRegistration;
    @Mock
    protected BGPPeerRegistry bgpPeerRegistry;
    @Mock
    protected ListenerRegistration<?> listener;
    @Mock
    protected Future<?> future;
    @Mock
    protected DOMDataWriteTransaction domDW;
    @Mock
    private ImportPolicyPeerTracker importPolicyPeerTracker;
    @Mock
    private DOMDataTreeChangeService dataTreeChangeService;
    protected static final RibId RIB_ID = new RibId("test");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(invocationOnMock -> {
            this.singletonService = (ClusterSingletonService) invocationOnMock.getArguments()[0];
            return this.singletonServiceRegistration;
        }).when(this.rib).registerClusterSingletonService(any(ClusterSingletonService.class));
        Mockito.doReturn(InstanceIdentifier.create(BgpRib.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight
                .params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib.class, new RibKey(RIB_ID))).when(this.rib)
                .getInstanceIdentifier();
        Mockito.doReturn(this.domTx).when(this.rib).createPeerChain(any(TransactionChainListener.class));
        Mockito.doReturn(AS).when(this.rib).getLocalAs();
        Mockito.doReturn(this.importPolicyPeerTracker).when(this.rib).getImportPolicyPeerTracker();
        Mockito.doNothing().when(this.importPolicyPeerTracker).peerRoleChanged(any(YangInstanceIdentifier.class), any(PeerRole.class));
        Mockito.doReturn(mock(AbstractImportPolicy.class)).when(this.importPolicyPeerTracker).policyFor(any(PeerId.class));
        Mockito.doReturn(mock(RIBSupportContextRegistry.class)).when(this.rib).getRibSupportContext();
        Mockito.doReturn(Collections.emptySet()).when(this.rib).getLocalTablesKeys();
        Mockito.doNothing().when(this.domTx).close();
        Mockito.doReturn(this.domDW).when(this.domTx).newWriteOnlyTransaction();
        Mockito.doNothing().when(this.domDW).put(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(MapEntryNode.class));
        Mockito.doNothing().when(this.domDW).delete(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class));
        Mockito.doNothing().when(this.domDW).merge(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        final CheckedFuture<?, ?> checkedFuture = mock(CheckedFuture.class);
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(checkedFuture).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doReturn(checkedFuture).when(this.domDW).submit();
        Mockito.doReturn(null).when(checkedFuture).checkedGet();
        Mockito.doReturn(null).when(checkedFuture).get();
        Mockito.doReturn(true).when(checkedFuture).isDone();
        Mockito.doReturn("checkedFuture").when(checkedFuture).toString();
        Mockito.doAnswer(invocationOnMock -> {
            this.singletonService.closeServiceInstance();
            return null;
        }).when(this.singletonServiceRegistration).close();
        Mockito.doReturn(YangInstanceIdentifier.of(Rib.QNAME)).when(this.rib).getYangRibId();
        Mockito.doReturn(this.dataTreeChangeService).when(this.rib).getService();
        Mockito.doReturn(this.listener).when(this.dataTreeChangeService).registerDataTreeChangeListener(any(), any());
        Mockito.doReturn(mock(ServiceGroupIdentifier.class)).when(this.rib).getRibIServiceGroupIdentifier();
        Mockito.doReturn(new BgpId("127.0.0.1")).when(this.rib).getBgpIdentifier();
        Mockito.doReturn(true).when(this.future).cancel(true);
        Mockito.doReturn(this.future).when(this.dispatcher)
                .createReconnectingClient(any(InetSocketAddress.class), anyInt(), any(KeyMapping.class));
        Mockito.doReturn(this.dispatcher).when(this.rib).getDispatcher();

        Mockito.doReturn(java.util.Optional.of(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)))
                .when(this.tableTypeRegistry).getTableType(any());
        Mockito.doReturn(java.util.Optional.of(TABLES_KEY)).when(this.tableTypeRegistry).getTableKey(any());
        Mockito.doReturn(Collections.singleton(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)))
                .when(this.rib).getLocalTables();
        Mockito.doNothing().when(this.configurationWriter).apply();

        Mockito.doNothing().when(this.bgpPeerRegistry).addPeer(any(IpAddress.class), any(BGPSessionListener.class), any(BGPSessionPreferences.class));
        Mockito.doNothing().when(this.bgpPeerRegistry).removePeer(any(IpAddress.class));
        Mockito.doReturn("registry").when(this.bgpPeerRegistry).toString();
        Mockito.doNothing().when(this.listener).close();
        Mockito.doReturn(this.bgpPeerRegistry).when(this.dispatcher).getBGPPeerRegistry();
    }
}
