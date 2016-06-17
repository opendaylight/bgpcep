/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeploymentManager implements AutoCloseable {

    //TODO logging
    private static final Logger LOG = LoggerFactory.getLogger(BgpDeploymentManager.class);

    private final Map<InstanceIdentifier<Protocol>, RIBImpl> ribs = new HashMap<>();
    private final Map<InstanceIdentifier<Protocol>, Map<NeighborKey, AutoCloseable>> peers = new HashMap<>();

    private final RIBExtensionConsumerContext extensions;
    private final BGPOpenConfigMappingService mappingService;
    private final BGPDispatcher dispatcher;
    private final BindingCodecTreeFactory codecFactory;
    private final DataBroker dataBroker;
    private final DOMDataBroker domDataBroker;
    private final SchemaService schemaService;
    private final RpcProviderRegistry rpcRegistry;
    private final BGPPeerRegistry peerRegistry;

    public BgpDeploymentManager(final RIBExtensionConsumerContext extensions, final BGPDispatcher dispatcher,
            final BGPOpenConfigMappingService mappingService, final BindingCodecTreeFactory codecFactory,
            final DataBroker dataBroker, final DOMDataBroker domDataBroker, final SchemaService schemaService,
            final RpcProviderRegistry rpcRegistry, final BGPPeerRegistry peerRegistry) {
        this.extensions = extensions;
        this.dispatcher = dispatcher;
        this.mappingService = mappingService;
        this.codecFactory = codecFactory;
        this.dataBroker = dataBroker;
        this.domDataBroker = domDataBroker;
        this.schemaService = schemaService;
        this.rpcRegistry = rpcRegistry;
        this.peerRegistry = peerRegistry;
    }

    @Override
    public void close() throws Exception {
        this.peers.values().forEach(peerMap -> peerMap.values().forEach(AutoCloseable::close));
        this.peers.clear();
        this.ribs.values().forEach(rib -> rib.close());
        this.ribs.clear();
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    public void onGlobalCreated(final InstanceIdentifier<Protocol> identifier, final Global global) {
        final RIBImpl newRib = createRib(global, identifier.firstKeyOf(Protocol.class).getName());
        this.schemaService.registerSchemaContextListener(newRib);
        this.ribs.put(identifier, newRib);
        this.peers.putIfAbsent(identifier, new HashMap<>());
    }

    public void onGlobalModified(final InstanceIdentifier<Protocol> identifier, final Bgp bgp) {
        //stop all deployed peer
        bgp.getNeighbors().getNeighbor().forEach(neighbor -> onNeighborRemoved(identifier, neighbor));
        onGlobalRemoved(identifier);
        onGlobalCreated(identifier, bgp.getGlobal());
        //recreate all peers
        bgp.getNeighbors().getNeighbor().forEach(neighbor -> onNeighborCreated(identifier, neighbor));
    }

    public void onGlobalRemoved(final InstanceIdentifier<Protocol> identifier) {
        final RIBImpl remove = this.ribs.remove(identifier);
        Preconditions.checkNotNull(remove);
        remove.close();
    }

    public void onNeighborCreated(final InstanceIdentifier<Protocol> identifier, final Neighbor neighbor) {
        final Map<NeighborKey, AutoCloseable> neighbors = this.peers.get(identifier);
        Preconditions.checkNotNull(neighbors);
        final RIBImpl ribImpl = this.ribs.get(identifier);
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress peerAddress =
                OpenConfigMappingUtil.getPeerAddress(neighbor.getNeighborAddress());
        if (!this.mappingService.isApplicationPeer(neighbor)) {
            onPeerCreated(neighbor, neighbors, ribImpl, peerAddress);
        } else {
            onApplicationPeerCreated(neighbor, neighbors, ribImpl);
        }
    }

    private void onPeerCreated(
            final Neighbor neighbor,
            final Map<NeighborKey, AutoCloseable> neighbors,
            final RIBImpl ribImpl,
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress peerAddress) {
        final BGPPeer newPeer = createBgpPeer(neighbor, ribImpl);
        final BGPSessionPreferences prefs = new BGPSessionPreferences(ribImpl.getLocalAs(), OpenConfigMappingUtil.getHoldTimer(neighbor), ribImpl.getBgpIdentifier(),
                new AsNumber(OpenConfigMappingUtil.getPeerAs(neighbor, ribImpl)), getBgpParameters(neighbor, ribImpl));
        this.peerRegistry.addPeer(peerAddress, newPeer, prefs);
        if (OpenConfigMappingUtil.isActive(neighbor)) {
            final InetSocketAddress inetSocketAddress = Ipv4Util.toInetSocketAddress(peerAddress, new PortNumber(179));
            this.dispatcher.createReconnectingClient(inetSocketAddress, this.peerRegistry,
                    OpenConfigMappingUtil.getRetryTimer(neighbor), OpenConfigMappingUtil.getKeys(neighbor, inetSocketAddress.getAddress()));
        }
        neighbors.put(neighbor.getKey(), new AutoCloseable() {
            @Override
            public void close() {
                newPeer.close();
                BgpDeploymentManager.this.peerRegistry.removePeer(peerAddress);
            }
        });
    }

    private void onApplicationPeerCreated(final Neighbor neighbor, final Map<NeighborKey, AutoCloseable> neighbors,
            final RIBImpl ribImpl) {
        final ApplicationPeer newPeer = OpenConfigMappingUtil.createAppPeer(neighbor, ribImpl);
        final YangInstanceIdentifier peerIId = OpenConfigMappingUtil.createApplicationPeerIId(newPeer);
        final DOMDataTreeChangeService service = (DOMDataTreeChangeService) this.domDataBroker.getSupportedExtensions().get(DOMDataTreeChangeService.class);
        final ListenerRegistration<ApplicationPeer> listenerRegistration =
                service.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, peerIId), newPeer);
        neighbors.put(neighbor.getKey(), new AutoCloseable() {
            @Override
            public void close() {
                listenerRegistration.close();
                newPeer.close();
            }
        });
    }

    public void onNeighborModified(final InstanceIdentifier<Protocol> identifier, final Neighbor neighbor) {
        onNeighborRemoved(identifier, neighbor);
        onNeighborCreated(identifier, neighbor);
    }

    public void onNeighborRemoved(final InstanceIdentifier<Protocol> identifier, final Neighbor neighbor) {
        final Map<NeighborKey, AutoCloseable> neighbors = this.peers.get(identifier);
        Preconditions.checkNotNull(neighbors);
        final NeighborKey neighborKey = neighbor.getKey();
        final AutoCloseable peer = neighbors.remove(neighborKey);
        Preconditions.checkNotNull(peer);
        try {
            peer.close();
        } catch (final Exception e) {
            LOG.warn("Failed to successfuly close Neighbor instance {}", neighborKey, e);
        }
    }

    public void closeInstances(final InstanceIdentifier<NetworkInstance> identifier) throws Exception {
        for (final AutoCloseable peer : this.peers.remove(identifier).values()) {
            peer.close();
        }
        this.ribs.remove(identifier).close();
    }

    private RIBImpl createRib(final Global global, final String bgpInstanceName) {
        final Map<TablesKey, PathSelectionMode> pathSelectionModes = this.mappingService.toPathSelectionMode(global.getAfiSafis().getAfiSafi()).entrySet()
                .stream().collect(Collectors.toMap(entry -> new TablesKey(entry.getKey().getAfi(), entry.getKey().getSafi()), entry -> entry.getValue()));
        return new RIBImpl(new RibId(bgpInstanceName), new AsNumber(global.getConfig().getAs().getValue()),
            new Ipv4Address(global.getConfig().getRouterId().getValue()), new Ipv4Address(global.getConfig().getRouterId().getValue()),
            this.extensions, this.dispatcher, this.codecFactory, this.dataBroker, this.domDataBroker,
            this.mappingService.toTableTypes(global.getAfiSafis().getAfiSafi()), pathSelectionModes,
            this.extensions.getClassLoadingStrategy());
    }

    private BGPPeer createBgpPeer(final Neighbor neighbor, final RIBImpl rib) {
        return new BGPPeer(OpenConfigMappingUtil.getNeighborAddress(neighbor.getNeighborAddress()), rib,
                this.mappingService.toPeerRole(neighbor), this.rpcRegistry);
    }

    private List<BgpParameters> getBgpParameters(final Neighbor neighbor, final RIB rib) {
        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> caps = new ArrayList<>();
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().setAs4BytesCapability(
            new As4BytesCapabilityBuilder().setAsNumber(rib.getLocalAs()).build()).build()).build());

        caps.add(new OptionalCapabilitiesBuilder().setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(MultiprotocolCapabilitiesUtil.RR_CAPABILITY).build());

        final List<AddressFamilies> addPathCapability = this.mappingService.toAddPathCapability(neighbor.getAfiSafis().getAfiSafi());
        if (!addPathCapability.isEmpty()) {
            caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setAddPathCapability(
                        new AddPathCapabilityBuilder().setAddressFamilies(addPathCapability).build()).build()).build()).build());
        }

        final List<BgpTableType> tableTypes = this.mappingService.toTableTypes(neighbor.getAfiSafis().getAfiSafi());
        for (final BgpTableType tableType : tableTypes) {
            if (!rib.getLocalTables().contains(tableType)) {
                LOG.info("RIB instance does not list {} in its local tables. Incoming data will be dropped.", tableType);
            }

            caps.add(new OptionalCapabilitiesBuilder().setCParameters(
                    new CParametersBuilder().addAugmentation(CParameters1.class,
                            new CParameters1Builder().setMultiprotocolCapability(
                                    new MultiprotocolCapabilityBuilder(tableType).build()).build()).build()).build());
        }
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(caps).build());
        return tlvs;
    }

}
