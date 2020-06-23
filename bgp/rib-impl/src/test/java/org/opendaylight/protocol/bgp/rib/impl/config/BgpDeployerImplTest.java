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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.impl.config.AbstractConfig.TABLES_KEY;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv4;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv6;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighbors;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighborsNoRR;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import io.netty.util.concurrent.Future;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.DefaultRibPoliciesMockTest;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class BgpDeployerImplTest extends DefaultRibPoliciesMockTest {
    private static final BgpTableType TABLE_TYPE = new BgpTableTypeImpl(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private static final String NETWORK_INSTANCE_NAME = "network-test";
    private static final KeyedInstanceIdentifier<NetworkInstance, NetworkInstanceKey> NETWORK_II =
            InstanceIdentifier.create(NetworkInstances.class)
                    .child(NetworkInstance.class, new NetworkInstanceKey(NETWORK_INSTANCE_NAME));
    private static final String KEY = "bgp";
    private static final InstanceIdentifier<Bgp> BGP_II = NETWORK_II.child(Protocols.class)
            .child(Protocol.class, new ProtocolKey(BGP.class, KEY))
            .augmentation(NetworkInstanceProtocol.class).child(Bgp.class);
    private static final InstanceIdentifier<Global> GLOBAL_II = BGP_II.child(Global.class);
    private static final InstanceIdentifier<Neighbors> NEIGHBORS_II = BGP_II.child(Neighbors.class);
    private static final int VERIFY_TIMEOUT_MILIS = 5000;

    @Mock
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Mock
    private DataTreeModification<Bgp> modification;
    @Mock
    private ClusterSingletonServiceProvider singletonServiceProvider;
    @Mock
    private RIBExtensionConsumerContext ribExtensionContext;
    @Mock
    private BGPDispatcher dispatcher;
    @Mock
    private CodecsRegistry codecsRegistry;
    @Mock
    private RpcProviderService rpcRegistry;
    @Mock
    private DOMDataTreeWriteTransaction transaction;
    @Mock
    private BGPPeerRegistry peerRegistry;
    private BgpDeployerImpl deployer;
    private DOMDataBroker domDataBroker;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        doReturn("mapping").when(this.tableTypeRegistry).toString();
        doReturn(Optional.of(TABLE_TYPE)).when(this.tableTypeRegistry).getTableType(any());
        doReturn(Optional.of(TABLES_KEY)).when(this.tableTypeRegistry).getTableKey(any());

        doReturn("bgpPeer").when(this.modification).toString();
//        ribExtensionContext = mock(RIBExtensionConsumerContext.class);
        doReturn(null).when(ribExtensionContext).getRIBSupport(any(TablesKey.class));

        final ClusterSingletonServiceRegistration serviceRegistration = mock(ClusterSingletonServiceRegistration.class);
        doAnswer(invocationOnMock -> {
            final ClusterSingletonService service = invocationOnMock.getArgument(0);
            service.instantiateServiceInstance();
            return serviceRegistration;
        }).when(this.singletonServiceProvider).registerClusterSingletonService(any());
        doNothing().when(serviceRegistration).close();

        final Future connectFuture = Mockito.mock(Future.class);
        doReturn(true).when(connectFuture).cancel(true);
        doReturn(connectFuture).when(this.dispatcher).createReconnectingClient(any(), any(), anyInt(), any());
        doReturn(this.peerRegistry).when(this.dispatcher).getBGPPeerRegistry();
        doNothing().when(this.peerRegistry).addPeer(any(), any(), any());
        doNothing().when(this.peerRegistry).removePeer(any());

        this.domDataBroker = spy(getDomBroker());
        doAnswer(broker -> {
            DOMTransactionChain transactionChain = mock(DOMTransactionChain.class);
            doNothing().when(transactionChain).close();
            doReturn(this.transaction).when(transactionChain).newWriteOnlyTransaction();
            return transactionChain;
        }).when(this.domDataBroker).createMergingTransactionChain(any());
        FluentFuture<CommitInfo> txFuture = FluentFuture.from(Futures.immediateFuture(null));
        doReturn(txFuture).when(this.transaction).commit();
        doNothing().when(this.transaction).put(any(), any(), any());
        doNothing().when(this.transaction).merge(any(), any(), any());
        doNothing().when(this.transaction).delete(any(), any());

        this.deployer = new BgpDeployerImpl(NETWORK_INSTANCE_NAME, this.singletonServiceProvider,
                getDataBroker(), this.tableTypeRegistry, this.ribExtensionContext, this.dispatcher, this.policyProvider,
            this.codecsRegistry, this.domDataBroker, this.rpcRegistry);
    }

    @Test
    public void testDeployerRib() throws Exception {
        deployer.init();
        checkPresentConfiguration(getDataBroker(), NETWORK_II);
        createRib(createGlobalIpv4());

        verify(this.singletonServiceProvider, timeout(VERIFY_TIMEOUT_MILIS).times(1))
                .registerClusterSingletonService(any());
        verify(this.domDataBroker, timeout(VERIFY_TIMEOUT_MILIS).times(1)).createMergingTransactionChain(any());
        verify(this.transaction, timeout(VERIFY_TIMEOUT_MILIS).times(1)).put(any(), any(), any());

        //change with same rib already existing
        createRib(createGlobalIpv4());
        verify(this.singletonServiceProvider, timeout(VERIFY_TIMEOUT_MILIS).times(1))
                .registerClusterSingletonService(any());
        verify(this.domDataBroker, timeout(VERIFY_TIMEOUT_MILIS).times(1)).createMergingTransactionChain(any());
        verify(this.transaction, timeout(VERIFY_TIMEOUT_MILIS).times(1)).put(any(), any(), any());

        //Update for existing rib
        createRib(createGlobalIpv6());
        verify(this.singletonServiceProvider, timeout(VERIFY_TIMEOUT_MILIS).times(1))
                .registerClusterSingletonService(any());
        verify(this.domDataBroker, timeout(VERIFY_TIMEOUT_MILIS).times(2)).createMergingTransactionChain(any());
        verify(this.transaction, timeout(VERIFY_TIMEOUT_MILIS).times(2)).put(any(), any(), any());
        verify(this.transaction, timeout(VERIFY_TIMEOUT_MILIS).times(1)).delete(any(), any());

        //Delete for existing rib
        deleteRib();

        verify(this.singletonServiceProvider, timeout(VERIFY_TIMEOUT_MILIS).times(1))
                .registerClusterSingletonService(any());
        verify(this.domDataBroker, timeout(VERIFY_TIMEOUT_MILIS).times(2)).createMergingTransactionChain(any());
        verify(this.transaction, timeout(VERIFY_TIMEOUT_MILIS).times(2)).put(any(), any(), any());
        verify(this.transaction, timeout(VERIFY_TIMEOUT_MILIS).times(2)).delete(any(), any());

        deployer.close();
    }

    @Test
    public void testDeployerCreateNeighbor() throws Exception {
        deployer.init();
        checkPresentConfiguration(getDataBroker(), NETWORK_II);

        createRib(createGlobalIpv4());
        createNeighbor(createNeighbors());
        verify(this.peerRegistry, timeout(VERIFY_TIMEOUT_MILIS).times(1)).addPeer(any(), any(), any());

        //change with same peer already existing
        createNeighbor(createNeighbors());
        verify(this.peerRegistry, timeout(VERIFY_TIMEOUT_MILIS).times(1)).addPeer(any(), any(), any());

        //Update for peer
        createNeighbor(createNeighborsNoRR());
        verify(this.peerRegistry, timeout(VERIFY_TIMEOUT_MILIS).times(2)).addPeer(any(), any(), any());
        verify(this.peerRegistry, timeout(VERIFY_TIMEOUT_MILIS).times(1)).removePeer(any());

        deleteNeighbors();
        verify(this.peerRegistry, timeout(VERIFY_TIMEOUT_MILIS).times(2)).removePeer(any());

        deployer.close();
    }

    private void createRib(final Global global) throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.mergeParentStructurePut(LogicalDatastoreType.CONFIGURATION, GLOBAL_II, global);
        wr.commit().get();
    }

    private void deleteRib() throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.delete(LogicalDatastoreType.CONFIGURATION, BGP_II);
        wr.commit().get();
    }

    private void createNeighbor(final Neighbors neighbors) throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.mergeParentStructurePut(LogicalDatastoreType.CONFIGURATION, NEIGHBORS_II, neighbors);
        wr.commit().get();
    }

    private void deleteNeighbors() throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.delete(LogicalDatastoreType.CONFIGURATION, NEIGHBORS_II);
        wr.commit().get();
    }
}
