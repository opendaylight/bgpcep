/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType.DELETE;
import static org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType.WRITE;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createAddPath;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createConfig;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createRR;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createTimers;
import static org.opendaylight.protocol.bgp.rib.impl.config.BgpPeerTest.createTransport;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

import com.google.common.primitives.Shorts;
import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.InstanceType;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AddPaths;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AddPathsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.RrClusterIdTypeBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.ProtocolsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BgpDeployerImpl.class})
public class BgpDeployerImplTest {
    private static final String NETWORK_INSTANCE_NAME = "network-test";
    private static final AsNumber AS = new AsNumber(72L);
    private static final IpAddress IPADDRESS = new IpAddress(new Ipv4Address("127.0.0.1"));
    private static final BgpId BGP_ID = new BgpId(IPADDRESS.getIpv4Address());
    private static final BgpTableType TABLE_TYPE = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private static final short SHORT = 0;

    @Mock
    DataObjectModification dObject;
    @Mock
    private BlueprintContainer blueprintContainer;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private BGPOpenConfigMappingService mappingService;
    @Mock
    private WriteTransaction wTx;
    @Mock
    private DataTreeModification<Bgp> modification;
    @Mock
    private ListenerRegistration dataTreeRegistration;
    @Mock
    private ServiceRegistration registration;

    private Collection<DataTreeModification<Bgp>> collection = Collections.singleton(modification);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final DOMDataBroker domDataBroker = mock(DOMDataBroker.class);

        Mockito.doReturn(this.wTx).when(this.dataBroker).newWriteOnlyTransaction();
        Mockito.doReturn("mapping").when(this.mappingService).toString();
        Mockito.doReturn(false).when(this.mappingService).isApplicationPeer(any(Neighbor.class));
        Mockito.doReturn(PeerRole.RrClient).when(this.mappingService).toPeerRole(any(Neighbor.class));
        Mockito.doReturn(Collections.singletonList(new AddressFamiliesBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
            .setSendReceive(SendReceive.Both).build())).when(this.mappingService).toAddPathCapability(any(List.class));

        Mockito.doReturn(null).when(domDataBroker).createTransactionChain(any());
        Mockito.doReturn(Collections.singletonMap(DOMDataTreeChangeService.class, mock(DOMDataBrokerExtension.class)))
            .when(domDataBroker).getSupportedExtensions();

        Mockito.doReturn(Collections.singletonList(TABLE_TYPE)).when(this.mappingService).toTableTypes(any());
        Mockito.doReturn(Collections.singletonMap(TABLE_TYPE, new AllPathSelection()))
            .when(this.mappingService).toPathSelectionMode(any());
        Mockito.doNothing().when(this.registration).unregister();
        Mockito.doReturn(this.registration).when(this.bundleContext).registerService(eq(InstanceType.RIB.getServices()), any()
            , any(Dictionary.class));
        Mockito.doReturn(this.registration).when(this.bundleContext).registerService(eq(InstanceType.PEER.getServices()), any()
            , any(Dictionary.class));


        Mockito.doNothing().when(this.wTx).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(NetworkInstance.class));
        final CheckedFuture future = mock(CheckedFuture.class);
        Mockito.doReturn(future).when(this.wTx).submit();
        Mockito.doNothing().when(future).addListener(any(), any());
        Mockito.doReturn(this.dataTreeRegistration).when(this.dataBroker).registerDataTreeChangeListener(any(), any());
        Mockito.doNothing().when(this.dataTreeRegistration).close();

        final InstanceIdentifier<Bgp> bgpIID = InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class, new NetworkInstanceKey(NETWORK_INSTANCE_NAME)).child(Protocols.class)
            .child(Protocol.class, new ProtocolKey(BGP.class, "bgp")).augmentation(Protocol1.class).child(Bgp.class);

        Mockito.doReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, bgpIID)).when(this.modification).getRootPath();
        Mockito.doReturn(this.dObject).when(this.modification).getRootNode();
        Mockito.doReturn("bgpPeer").when(this.modification).toString();

        Mockito.doReturn(Collections.singleton(this.dObject)).when(this.dObject).getModifiedChildren();
        Mockito.doReturn("dObject").when(this.dObject).toString();

        final RIBExtensionConsumerContext extension = mock(RIBExtensionConsumerContext.class);
        Mockito.doReturn(mock(GeneratedClassLoadingStrategy.class)).when(extension).getClassLoadingStrategy();

        final ClusterSingletonServiceProvider singletonServiceProvider = mock(ClusterSingletonServiceProvider.class);
        final ClusterSingletonServiceRegistration serviceRegistration = mock(ClusterSingletonServiceRegistration.class);
        Mockito.doReturn(serviceRegistration).when(singletonServiceProvider).registerClusterSingletonService(any());
        Mockito.doNothing().when(serviceRegistration).close();

        final SchemaService schemaService = mock(SchemaService.class);
        Mockito.doNothing().when(this.dataTreeRegistration).close();

        Mockito.doReturn(this.dataTreeRegistration).when(schemaService).registerSchemaContextListener(any());

        final RibImpl ribImpl = new RibImpl(singletonServiceProvider, extension,
            mock(BGPDispatcher.class), mock(BindingCodecTreeFactory.class), domDataBroker, schemaService);
        Mockito.doReturn(ribImpl).when(this.blueprintContainer).getComponentInstance(eq("ribImpl"));

        final BgpPeer bgpPeer = new BgpPeer(mock(RpcProviderRegistry.class), mock(BGPPeerRegistry.class));
        Mockito.doReturn(bgpPeer).when(this.blueprintContainer).getComponentInstance(eq("bgpPeer"));
        this.collection = Collections.singleton(modification);
    }

    @Test
    public void testDeployerRib() throws Exception {
        Mockito.doReturn(Global.class).when(this.dObject).getDataType();
        final BgpDeployerImpl deployer = new BgpDeployerImpl(NETWORK_INSTANCE_NAME, this.blueprintContainer, this.bundleContext, this.dataBroker, this.mappingService);
        final BgpDeployerImpl spyDeployer = spy(deployer);
        configureGlobal(IPV4UNICAST.class);
        Mockito.doReturn(WRITE).when(this.dObject).getModificationType();

        final KeyedInstanceIdentifier<NetworkInstance, NetworkInstanceKey> networkInstanceIId = InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class, new NetworkInstanceKey(NETWORK_INSTANCE_NAME));
        final NetworkInstance netII = new NetworkInstanceBuilder().setName(networkInstanceIId.firstKeyOf(NetworkInstance.class).getName())
            .setProtocols(new ProtocolsBuilder().build()).build();
        verify(this.wTx).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), Mockito.eq(netII));
        verify(this.dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.any(BgpDeployerImpl.class));

        assertEquals(networkInstanceIId, spyDeployer.getInstanceIdentifier());
        assertEquals(this.mappingService, spyDeployer.getMappingService());

        spyDeployer.onDataTreeChanged(this.collection);
        verifyPrivate(spyDeployer).invoke("onGlobalChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext).registerService(eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));
        verify(spyDeployer).onDataTreeChanged(any(Collection.class));
        verify(spyDeployer).onGlobalModified(any(InstanceIdentifier.class), any(), any());
        verifyPrivate(spyDeployer).invoke("onGlobalCreated", any(InstanceIdentifier.class), any(Global.class), any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("initiateRibInstance", any(InstanceIdentifier.class), any(Global.class), any(RibImpl.class), any(BgpDeployer
            .WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("registerRibInstance", any(RibImpl.class), anyString());
        verify(spyDeployer).onDataTreeChanged(any(Collection.class));

        //change with same rib already existing
        spyDeployer.onDataTreeChanged(this.collection);
        verifyPrivate(spyDeployer, times(2)).invoke("onGlobalChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext).registerService(eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));
        verify(spyDeployer, times(2)).onDataTreeChanged(any(Collection.class));
        verify(spyDeployer, times(2)).onGlobalModified(any(InstanceIdentifier.class), any(), any());
        verifyPrivate(spyDeployer).invoke("onGlobalCreated", any(InstanceIdentifier.class), any(Global.class), any(BgpDeployer
            .WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("initiateRibInstance", any(InstanceIdentifier.class), any(Global.class), any(RibImpl.class), any(BgpDeployer
            .WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("registerRibInstance", any(RibImpl.class), anyString());
        verify(spyDeployer, times(2)).onDataTreeChanged(any(Collection.class));

        //Update for existing rib
        configureGlobal(IPV6UNICAST.class);
        spyDeployer.onDataTreeChanged(this.collection);
        verifyPrivate(spyDeployer, times(3)).invoke("onGlobalChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext, times(2)).registerService(eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));
        verify(spyDeployer, times(3)).onDataTreeChanged(any(Collection.class));
        verify(spyDeployer, times(3)).onGlobalModified(any(InstanceIdentifier.class), any(), any());
        verifyPrivate(spyDeployer).invoke("onGlobalCreated", any(InstanceIdentifier.class), any(Global.class),
            any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("onGlobalUpdated", any(InstanceIdentifier.class), any(Global.class), any(RibImpl.class), any(BgpDeployer.WriteConfiguration.class));
        verify(this.dataTreeRegistration).close();
        verify(this.registration).unregister();
        verifyPrivate(spyDeployer).invoke("closeAllBindedPeers", any(InstanceIdentifier.class));
        verifyPrivate(spyDeployer, times(2)).invoke("initiateRibInstance", any(InstanceIdentifier.class), any(Global.class), any(RibImpl.class), any
            (BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer, times(2)).invoke("registerRibInstance", any(RibImpl.class), anyString());
        verify(spyDeployer, times(3)).onDataTreeChanged(any(Collection.class));

        //Delete for existing rib
        Mockito.doReturn(DELETE).when(this.dObject).getModificationType();

        spyDeployer.onDataTreeChanged(this.collection);
        verifyPrivate(spyDeployer, times(4)).invoke("onGlobalChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext, times(2)).registerService(eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));
        verify(spyDeployer, times(4)).onDataTreeChanged(any(Collection.class));
        verify(spyDeployer, times(3)).onGlobalModified(any(InstanceIdentifier.class), any(), any());
        verify(spyDeployer).onGlobalRemoved(any(InstanceIdentifier.class));
        verify(this.dataTreeRegistration, times(2)).close();
        verify(this.registration, times(2)).unregister();
        verifyPrivate(spyDeployer).invoke("onGlobalCreated", any(InstanceIdentifier.class), any(Global.class),
            any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("onGlobalUpdated", any(InstanceIdentifier.class), any(Global.class), any(RibImpl.class), any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("closeAllBindedPeers", any(InstanceIdentifier.class));
        verifyPrivate(spyDeployer, times(2)).invoke("initiateRibInstance", any(InstanceIdentifier.class), any(Global.class), any(RibImpl.class), any
            (BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer, times(2)).invoke("registerRibInstance", any(RibImpl.class), anyString());
        verify(spyDeployer, times(4)).onDataTreeChanged(any(Collection.class));

        deployer.close();
    }

    private void configureGlobal(final Class<? extends AfiSafiType> afi) {
        final Config config = new ConfigBuilder().setAs(AS).setRouterId(BGP_ID).build();
        final ArrayList<AfiSafi> afiSafiList = new ArrayList<>();
        afiSafiList.add(new AfiSafiBuilder().setAfiSafiName(afi).addAugmentation(AfiSafi2.class, new AfiSafi2Builder().setReceive(true)
            .setSendMax(Shorts.checkedCast(0L)).build()).build());
        final AfiSafis afiSafi = new AfiSafisBuilder().setAfiSafi(afiSafiList).build();
        Mockito.doReturn(new GlobalBuilder().setConfig(config).setAfiSafis(afiSafi).build()).when(this.dObject).getDataAfter();
    }

    /**
     * Test create Rib
     */
    @Test
    public void testDeployerCreateNeighbor() throws Exception {

        final BgpDeployerImpl deployer = new BgpDeployerImpl(NETWORK_INSTANCE_NAME, this.blueprintContainer, this.bundleContext, this.dataBroker, this.mappingService);
        final BgpDeployerImpl spyDeployer = spy(deployer);

        //First create Rib
        Mockito.doReturn(Global.class).when(this.dObject).getDataType();
        Mockito.doReturn(WRITE).when(this.dObject).getModificationType();
        configureGlobal(IPV4UNICAST.class);

        spyDeployer.onDataTreeChanged(this.collection);
        verifyPrivate(spyDeployer).invoke("onGlobalChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(this.blueprintContainer).getComponentInstance(eq("ribImpl"));
        verify(this.bundleContext).registerService(eq(InstanceType.RIB.getServices()), any(), any(Dictionary.class));
        verify(spyDeployer).onDataTreeChanged(any(Collection.class));
        verify(spyDeployer).onGlobalModified(any(InstanceIdentifier.class), any(), any());
        verifyPrivate(spyDeployer).invoke("onGlobalCreated", any(InstanceIdentifier.class), any(Global.class), any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("initiateRibInstance", any(InstanceIdentifier.class), any(Global.class), any(RibImpl.class), any(BgpDeployer
            .WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("registerRibInstance", any(RibImpl.class), anyString());
        verify(spyDeployer).onDataTreeChanged(any(Collection.class));


        Mockito.doReturn(Neighbors.class).when(this.dObject).getDataType();
        Mockito.doReturn(WRITE).when(this.dObject).getModificationType();
        configureNeighbor(IPV4UNICAST.class);


        final KeyedInstanceIdentifier<NetworkInstance, NetworkInstanceKey> networkInstanceIId = InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class, new NetworkInstanceKey(NETWORK_INSTANCE_NAME));
        final NetworkInstance netII = new NetworkInstanceBuilder().setName(networkInstanceIId.firstKeyOf(NetworkInstance.class).getName())
            .setProtocols(new ProtocolsBuilder().build()).build();
        verify(this.wTx).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), Mockito.eq(netII));
        verify(this.dataBroker).registerDataTreeChangeListener(Mockito.any(DataTreeIdentifier.class), Mockito.any(BgpDeployerImpl.class));

        assertEquals(networkInstanceIId, spyDeployer.getInstanceIdentifier());
        assertEquals(this.mappingService, spyDeployer.getMappingService());

        spyDeployer.onDataTreeChanged(this.collection);
        verify(spyDeployer, times(2)).onDataTreeChanged(any(Collection.class));
        verifyPrivate(spyDeployer).invoke("onNeighborsChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(spyDeployer).onNeighborModified(any(InstanceIdentifier.class), any(Neighbor.class), any());
        verifyPrivate(spyDeployer).invoke("onNeighborCreated", any(InstanceIdentifier.class), any(Neighbor.class), any(BgpDeployer.WriteConfiguration.class));
        verify(this.blueprintContainer).getComponentInstance(eq("bgpPeer"));
        verifyPrivate(spyDeployer).invoke("initiatePeerInstance", any(InstanceIdentifier.class), any(InstanceIdentifier.class), any(Neighbor.class),
            any(PeerBean.class), any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("registerPeerInstance", any(Neighbor.class), anyString());
        verify(this.bundleContext).registerService(eq(InstanceType.PEER.getServices()), any(BgpPeer.class), any(Dictionary.class));

        //change with same peer already existing
        spyDeployer.onDataTreeChanged(this.collection);
        verify(spyDeployer, times(3)).onDataTreeChanged(any(Collection.class));
        verifyPrivate(spyDeployer, times(2)).invoke("onNeighborsChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(spyDeployer, times(2)).onNeighborModified(any(InstanceIdentifier.class), any(Neighbor.class), any());
        verifyPrivate(spyDeployer).invoke("onNeighborCreated", any(InstanceIdentifier.class), any(Neighbor.class), any(BgpDeployer.WriteConfiguration.class));
        verify(this.blueprintContainer).getComponentInstance(eq("bgpPeer"));
        verifyPrivate(spyDeployer).invoke("initiatePeerInstance", any(InstanceIdentifier.class), any(InstanceIdentifier.class), any(Neighbor.class),
            any(PeerBean.class), any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer).invoke("registerPeerInstance", any(Neighbor.class), anyString());
        verify(this.bundleContext).registerService(eq(InstanceType.PEER.getServices()), any(BgpPeer.class), any(Dictionary.class));

        //Update for existing rib
        configureNeighbor(IPV6UNICAST.class);
        spyDeployer.onDataTreeChanged(this.collection);
        verify(spyDeployer, times(4)).onDataTreeChanged(any(Collection.class));
        verifyPrivate(spyDeployer, times(3)).invoke("onNeighborsChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(spyDeployer, times(3)).onNeighborModified(any(InstanceIdentifier.class), any(Neighbor.class), any());
        verifyPrivate(spyDeployer).invoke("onNeighborCreated", any(InstanceIdentifier.class), any(Neighbor.class), any(BgpDeployer.WriteConfiguration.class));
        verify(this.blueprintContainer).getComponentInstance(eq("bgpPeer"));
        verifyPrivate(spyDeployer, times(2)).invoke("initiatePeerInstance", any(InstanceIdentifier.class), any(InstanceIdentifier.class), any(Neighbor.class),
            any(PeerBean.class), any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer, times(2)).invoke("registerPeerInstance", any(Neighbor.class), anyString());
        verify(this.bundleContext, times(2)).registerService(eq(InstanceType.PEER.getServices()), any(BgpPeer.class), any(Dictionary.class));

       //Delete existing Peer
        Mockito.doReturn(DELETE).when(this.dObject).getModificationType();

        spyDeployer.onDataTreeChanged(this.collection);
        verify(spyDeployer, times(5)).onDataTreeChanged(any(Collection.class));
        verify( this.dObject).getDataBefore();
        verifyPrivate(spyDeployer, times(4)).invoke("onNeighborsChanged", any(DataObjectModification.class), any(InstanceIdentifier.class));
        verify(spyDeployer, times(3)).onNeighborModified(any(InstanceIdentifier.class), any(Neighbor.class), any());
        verify(spyDeployer, times(1)).onNeighborRemoved(any(InstanceIdentifier.class), any(Neighbor.class));
        verifyPrivate(spyDeployer).invoke("onNeighborCreated", any(InstanceIdentifier.class), any(Neighbor.class), any(BgpDeployer.WriteConfiguration.class));
        verify(this.blueprintContainer).getComponentInstance(eq("bgpPeer"));
        verifyPrivate(spyDeployer, times(2)).invoke("initiatePeerInstance", any(InstanceIdentifier.class), any(InstanceIdentifier.class), any(Neighbor.class),
            any(PeerBean.class), any(BgpDeployer.WriteConfiguration.class));
        verifyPrivate(spyDeployer, times(2)).invoke("registerPeerInstance", any(Neighbor.class), anyString());
        verify(this.bundleContext, times(2)).registerService(eq(InstanceType.PEER.getServices()), any(BgpPeer.class), any(Dictionary.class));

        deployer.close();
    }

    private void configureNeighbor(final Class<? extends AfiSafiType> afi) {
        Mockito.doReturn(createNeighborExpected(afi)).when(this.dObject).getDataAfter();
        Mockito.doReturn(createNeighborExpected(afi)).when(this.dObject).getDataBefore();
    }


   private Neighbor createNeighborExpected(final Class<? extends AfiSafiType> afi) {
        return new NeighborBuilder()
            .setAfiSafis(createAfiSafi(afi))
            .setConfig(createConfig())
            .setNeighborAddress(IPADDRESS)
            .setRouteReflector(createRR())
            .setTimers(createTimers())
            .setTransport(createTransport())
            .setAddPaths(createAddPath())
            .build();
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis createAfiSafi(final Class<? extends AfiSafiType> afi) {
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder()
            .setAfiSafi(Collections.singletonList(new AfiSafiBuilder().setAfiSafiName(afi)
            .addAugmentation(AfiSafi1.class, new AfiSafi1Builder().setReceive(true).setSendMax(SHORT).build()).build())).build();
    }
}