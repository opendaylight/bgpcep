/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.impl.config.AbstractConfig.TABLES_KEY;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv4;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createGlobalIpv6;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighbors;
import static org.opendaylight.protocol.bgp.rib.impl.config.RIBTestsUtil.createNeighborsNoRR;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import io.netty.util.concurrent.Future;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.BGPRibRoutingPolicyFactoryImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.DefaultRibPoliciesMockTest;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
    private ListenerRegistration<?> dataTreeRegistration;
    @Mock
    private ClusterSingletonServiceProvider singletonServiceProvider;
    @Mock
    private BGPDispatcher dispatcher;
    @Mock
    private BindingCodecTreeFactory codecFactory;
    @Mock
    private DOMSchemaService schemaService;
    @Mock
    private RpcProviderRegistry rpcRegistry;
    @Mock
    private BGPPeerRegistry peerRegistry;
    private BgpDeployerImpl deployer;


    @Before
    public void setUp() throws Exception {
        super.setUp();

        doReturn("mapping").when(this.tableTypeRegistry).toString();
        doReturn(Optional.of(TABLE_TYPE)).when(this.tableTypeRegistry).getTableType(any());
        doReturn(Optional.of(TABLES_KEY)).when(this.tableTypeRegistry).getTableKey(any());

        doNothing().when(this.dataTreeRegistration).close();
        doReturn("bgpPeer").when(this.modification).toString();
        final RIBExtensionConsumerContext extension = mock(RIBExtensionConsumerContext.class);
        doReturn(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy()).when(extension).getClassLoadingStrategy();

        final ClusterSingletonServiceRegistration serviceRegistration = mock(ClusterSingletonServiceRegistration.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final ClusterSingletonService service = invocationOnMock.getArgumentAt(0, ClusterSingletonService.class);
                service.instantiateServiceInstance();
                return serviceRegistration;
            }
        }).when(this.singletonServiceProvider).registerClusterSingletonService(any(ClusterSingletonService.class));
        doNothing().when(serviceRegistration).close();

        schemaService = mock(DOMSchemaService.class);
        doNothing().when(this.dataTreeRegistration).close();

        doReturn(this.dataTreeRegistration).when(schemaService).registerSchemaContextListener(any());

        final DataBroker dataBroker = getDataBroker();
        final DOMDataBroker domBroker = getDomBroker();
        final SimpleRIBExtensionProviderContext ribExtensionContext = new SimpleRIBExtensionProviderContext();
        final BGPRibRoutingPolicyFactoryImpl policyFactory = new BGPRibRoutingPolicyFactoryImpl(dataBroker, new StatementRegistry());
        doReturn(this.peerRegistry).when(this.dispatcher).getBGPPeerRegistry();
        final Future future = Mockito.mock(Future.class);
        doReturn(true).when(future).cancel(true);
        doReturn(future).when(this.dispatcher).createReconnectingClient(any(), any(), Matchers.anyInt(), any());
        doNothing().when(this.peerRegistry).addPeer(any(), any(), any());
        doNothing().when(this.peerRegistry).removePeer(any());
        this.deployer = new BgpDeployerImpl(NETWORK_INSTANCE_NAME, this.singletonServiceProvider, dataBroker,
                this.tableTypeRegistry, ribExtensionContext, dispatcher, policyFactory, codecFactory, domBroker, schemaService, rpcRegistry);
    }

    @Test
    public void testDeployerRib() throws Exception {
        deployer.init();
        checkPresentConfiguration(getDataBroker(), NETWORK_II);
        createRib(createGlobalIpv4());
        verify(this.schemaService, timeout(VERIFY_TIMEOUT_MILIS)).registerSchemaContextListener(any());

        //change with same rib already existing
        createRib(createGlobalIpv4());
        verify(this.schemaService, timeout(VERIFY_TIMEOUT_MILIS)).registerSchemaContextListener(any());

        //Update for existing rib
        createRib(createGlobalIpv6());
        verify(this.dataTreeRegistration, timeout(VERIFY_TIMEOUT_MILIS)).close();

        //Delete for existing rib
        deleteRib();
        verify(this.dataTreeRegistration, timeout(VERIFY_TIMEOUT_MILIS).times(2)).close();

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