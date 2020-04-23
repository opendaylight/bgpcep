/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.impl.config.AbstractConfig.TABLES_KEY;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv4;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv6;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighbors;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighborsNoRR;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import java.util.Dictionary;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.binding.runtime.spi.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.DefaultRibPoliciesMockTest;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.InstanceType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;

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
    private BlueprintContainer blueprintContainer;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Mock
    private DataTreeModification<Bgp> modification;
    @Mock
    private ListenerRegistration<?> dataTreeRegistration;
    @Mock
    private ServiceRegistration<?> registration;
    @Mock
    private ClusterSingletonServiceProvider singletonServiceProvider;
    private BgpDeployerImpl deployer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        doReturn("mapping").when(this.tableTypeRegistry).toString();
        doReturn(Optional.of(TABLE_TYPE)).when(this.tableTypeRegistry).getTableType(any());
        doReturn(Optional.of(TABLES_KEY)).when(this.tableTypeRegistry).getTableKey(any());
        doNothing().when(this.registration).unregister();
        doReturn(this.registration).when(this.bundleContext).registerService(eq(InstanceType.RIB.getServices()),
                any(), any(Dictionary.class));
        doReturn(this.registration).when(this.bundleContext).registerService(eq(InstanceType.PEER.getServices()),
                any(), any(Dictionary.class));

        doNothing().when(this.dataTreeRegistration).close();
        doReturn("bgpPeer").when(this.modification).toString();
        final RIBExtensionConsumerContext extension = mock(RIBExtensionConsumerContext.class);
        doReturn(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy()).when(extension).getClassLoadingStrategy();

        final ClusterSingletonServiceRegistration serviceRegistration = mock(ClusterSingletonServiceRegistration.class);
        doReturn(serviceRegistration).when(this.singletonServiceProvider).registerClusterSingletonService(any());
        doNothing().when(serviceRegistration).close();

        schemaService = mock(DOMSchemaService.class);
        doNothing().when(this.dataTreeRegistration).close();

        doReturn(this.dataTreeRegistration).when(schemaService).registerSchemaContextListener(any());

        final RibImpl ribImpl = new RibImpl(extension, mock(BGPDispatcher.class), this.policyProvider,
            mock(BindingCodecTreeFactory.class), getDomBroker(), getDataBroker(), schemaService);
        doReturn(ribImpl).when(this.blueprintContainer).getComponentInstance(eq("ribImpl"));

        doReturn(new BgpPeer(mock(RpcProviderService.class))).when(this.blueprintContainer)
            .getComponentInstance(eq("bgpPeer"));

        this.deployer = new BgpDeployerImpl(NETWORK_INSTANCE_NAME, this.singletonServiceProvider,
            this.blueprintContainer, this.bundleContext, getDataBroker(), this.tableTypeRegistry);
    }

    @Test
    public void testDeployerRib() throws Exception {
        deployer.init();
        checkPresentConfiguration(getDataBroker(), NETWORK_II);
        createRib(createGlobalIpv4());

        verify(this.blueprintContainer, timeout(VERIFY_TIMEOUT_MILIS)).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext, timeout(VERIFY_TIMEOUT_MILIS)).registerService(eq(InstanceType.RIB.getServices()),
            any(), any(Dictionary.class));

        //change with same rib already existing
        createRib(createGlobalIpv4());
        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext).registerService(eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));

        //Update for existing rib
        createRib(createGlobalIpv6());

        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext, timeout(VERIFY_TIMEOUT_MILIS).times(2)).registerService(
            eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));
        verify(this.dataTreeRegistration).close();
        verify(this.registration).unregister();

        //Delete for existing rib
        deleteRib();

        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext, timeout(VERIFY_TIMEOUT_MILIS).times(2))
                .registerService(eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));
        verify(this.dataTreeRegistration, timeout(VERIFY_TIMEOUT_MILIS).times(2)).close();
        verify(this.registration, timeout(VERIFY_TIMEOUT_MILIS).times(2)).unregister();

        deployer.close();
    }

    @Test
    public void testDeployerCreateNeighbor() throws Exception {
        deployer.init();
        checkPresentConfiguration(getDataBroker(), NETWORK_II);

        createRib(createGlobalIpv4());
        createNeighbor(createNeighbors());
        verify(this.blueprintContainer, timeout(VERIFY_TIMEOUT_MILIS)).getComponentInstance(eq("bgpPeer"));
        verify(this.bundleContext, timeout(VERIFY_TIMEOUT_MILIS)).registerService(eq(InstanceType.PEER.getServices()),
                any(BgpPeer.class), any(Dictionary.class));

        //change with same peer already existing
        createNeighbor(createNeighbors());
        verify(this.blueprintContainer).getComponentInstance(eq("bgpPeer"));
        verify(this.bundleContext).registerService(eq(InstanceType.PEER.getServices()),
                any(BgpPeer.class), any(Dictionary.class));

        //Update for peer
        createNeighbor(createNeighborsNoRR());
        verify(this.blueprintContainer).getComponentInstance(eq("bgpPeer"));
        verify(this.bundleContext, timeout(VERIFY_TIMEOUT_MILIS).times(2))
                .registerService(eq(InstanceType.PEER.getServices()), any(BgpPeer.class), any(Dictionary.class));
        verify(this.registration).unregister();

        deleteNeighbors();
        //Delete existing Peer
        verify(this.bundleContext, times(2))
                .registerService(eq(InstanceType.PEER.getServices()), any(BgpPeer.class), any(Dictionary.class));
        verify(this.registration, timeout(VERIFY_TIMEOUT_MILIS).times(2)).unregister();

        deployer.close();
    }

    private void createRib(final Global global) throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.put(LogicalDatastoreType.CONFIGURATION, GLOBAL_II, global, true);
        wr.commit().get();
    }

    private void deleteRib() throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.delete(LogicalDatastoreType.CONFIGURATION, BGP_II);
        wr.commit().get();
    }

    private void createNeighbor(final Neighbors neighbors) throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.put(LogicalDatastoreType.CONFIGURATION, NEIGHBORS_II, neighbors, true);
        wr.commit().get();
    }

    private void deleteNeighbors() throws ExecutionException, InterruptedException {
        final WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.delete(LogicalDatastoreType.CONFIGURATION, NEIGHBORS_II);
        wr.commit().get();
    }
}
