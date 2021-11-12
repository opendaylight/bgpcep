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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.impl.config.AbstractConfig.TABLES_KEY;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv4;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv6;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighbors;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighborsNoRR;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import io.netty.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.DefaultBGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.DefaultRibPoliciesMockTest;
import org.opendaylight.protocol.bgp.rib.impl.protocol.BGPReconnectPromise;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPStateCollector;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class BgpDeployerTest extends DefaultRibPoliciesMockTest {

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
    private BGPDispatcher dispatcher;
    @Mock
    private CodecsRegistry codecsRegistry;
    @Mock
    private RpcProviderService rpcRegistry;
    @Mock
    private RIBExtensionConsumerContext extensionContext;

    @Mock
    private ClusterSingletonServiceProvider singletonServiceProvider;

    private final BGPStateProviderRegistry stateProviderRegistry = new BGPStateCollector();
    private DefaultBgpDeployer deployer;
    private BGPClusterSingletonService spiedBgpSingletonService;
    private CountDownLatch bgpSingletonObtainedLatch;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        doReturn("mapping").when(this.tableTypeRegistry).toString();
        doReturn(TABLE_TYPE).when(this.tableTypeRegistry).getTableType(any());
        doReturn(TABLES_KEY).when(this.tableTypeRegistry).getTableKey(any());

        final ClusterSingletonServiceRegistration serviceRegistration = mock(ClusterSingletonServiceRegistration.class);
        doReturn(serviceRegistration).when(this.singletonServiceProvider).registerClusterSingletonService(any());
        doNothing().when(serviceRegistration).close();

        final Future future = mock(BGPReconnectPromise.class);
        doReturn(true).when(future).cancel(true);
        doReturn(future).when(this.dispatcher).createReconnectingClient(any(), any(), anyInt(), any());
        this.deployer = spy(new DefaultBgpDeployer(NETWORK_INSTANCE_NAME, this.singletonServiceProvider,
                this.rpcRegistry, this.extensionContext, this.dispatcher,
                new DefaultBGPRibRoutingPolicyFactory(getDataBroker(), new StatementRegistry()),
                this.codecsRegistry, getDomBroker(), getDataBroker(), this.tableTypeRegistry, stateProviderRegistry));
        this.bgpSingletonObtainedLatch = new CountDownLatch(1);
        doAnswer(invocationOnMock -> {
                final BGPClusterSingletonService real =
                        (BGPClusterSingletonService) invocationOnMock.callRealMethod();
                if (this.spiedBgpSingletonService == null) {
                    this.spiedBgpSingletonService = spy(real);
                }
                this.bgpSingletonObtainedLatch.countDown();
                return this.spiedBgpSingletonService;
            }
        ).when(this.deployer).getBgpClusterSingleton(any());
    }

    @Test
    public void testDeployerRib() throws Exception {
        this.deployer.init();
        checkPresentConfiguration(getDataBroker(), NETWORK_II);
        createRib(createGlobalIpv4());
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(1))
                .initiateRibInstance(any());

        //change with same rib already existing
        createRib(createGlobalIpv4());
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(1))
                .initiateRibInstance(any());

        //Update for existing rib
        createRib(createGlobalIpv6());
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(2))
                .initiateRibInstance(any());
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(1))
                .closeRibInstance();

        //Delete for existing rib
        deleteRib();
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(2))
                .initiateRibInstance(any());
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(2))
                .closeRibInstance();

        this.deployer.close();
    }

    @Test
    public void testDeployerCreateNeighbor() throws Exception {
        this.deployer.init();
        checkPresentConfiguration(getDataBroker(), NETWORK_II);

        createRib(createGlobalIpv4());
        createNeighbor(createNeighbors());
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS)).onNeighborCreated(any());

        //change with same peer already existing
        createNeighbor(createNeighbors());
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS)).onNeighborCreated(any());
        verify(this.spiedBgpSingletonService, never()).onNeighborRemoved(any());
        verify(this.spiedBgpSingletonService, never()).onNeighborUpdated(any(), any());

        //Update for peer
        createNeighbor(createNeighborsNoRR());
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(1)).onNeighborUpdated(any(), any());

        deleteNeighbors();
        //Delete existing Peer
        awaitForObtainedSingleton();
        verify(this.spiedBgpSingletonService, timeout(VERIFY_TIMEOUT_MILIS).times(1)).onNeighborRemoved(any());
        this.deployer.close();
    }

    private void awaitForObtainedSingleton() throws InterruptedException {
        this.bgpSingletonObtainedLatch = new CountDownLatch(1);
        this.bgpSingletonObtainedLatch.await(VERIFY_TIMEOUT_MILIS, TimeUnit.MILLISECONDS);
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
