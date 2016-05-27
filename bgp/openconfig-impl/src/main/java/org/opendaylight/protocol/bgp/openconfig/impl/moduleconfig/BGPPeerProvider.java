/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.AddPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.AddPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.AdvertizedTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.AdvertizedTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.RibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.RpcRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.peer.rev160606.bgp.peer.config.RpcRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.spi.rev160606.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.spi.rev160606.RibInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.binding.rev131028.BindingRpcRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPPeerProvider {

    private static final String PEER = "peer_";

    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerProvider.class);

    private static final Function<String, AdvertizedTable> ADVERTIZED_TABLE_FUNCTION = new Function<String, AdvertizedTable>() {
        @Override
        public AdvertizedTable apply(final String instance) {
            return new AdvertizedTableBuilder().setName(instance).setType(BgpTableType.class).build();
        }
    };

    private static final Function<String, Rib> TO_RIB_FUNCTION = new Function<String, Rib>() {
        @Override
        public Rib apply(final String name) {
            return new RibBuilder().setName(name).setType(RibInstance.class).build();
        }
    };

    private static final Function<String, RpcRegistry> RPC_REG_FUNCTION = new Function<String, RpcRegistry>() {
        @Override
        public RpcRegistry apply(final String name) {
            return new RpcRegistryBuilder().setName(name).setType(BindingRpcRegistry.class).build();
        }
    };

    private static final Function<String, AddPath> ADD_PATH_FUNCTION = new Function<String, AddPath>() {
        @Override
        public AddPath apply(final String name) {
            return new AddPathBuilder()
                .setName(name)
                .setType(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.spi.rev160606.AddPath.class)
                .build();
        }
    };

    private final BGPConfigHolder<Neighbor> neighborState;
    private final BGPConfigHolder<Bgp> globalState;
    private final BGPConfigModuleProvider configModuleOp;
    private final DataBroker dataBroker;

    public BGPPeerProvider(final BGPConfigStateStore configHolders, final BGPConfigModuleProvider configModuleWriter, final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.configModuleOp = Preconditions.checkNotNull(configModuleWriter);
        this.globalState = Preconditions.checkNotNull(configHolders.getBGPConfigHolder(Bgp.class));
        this.neighborState = Preconditions.checkNotNull(configHolders.getBGPConfigHolder(Neighbor.class));
    }

    public void onNeighborRemoved(final Neighbor removedNeighbor) {
        final ModuleKey moduleKey = this.neighborState.getModuleKey(removedNeighbor.getKey());
        if (moduleKey != null) {
            try {
                final ReadWriteTransaction rwTx = this.dataBroker.newReadWriteTransaction();
                final Optional<Module> maybeModule = this.configModuleOp.readModuleConfiguration(moduleKey, rwTx);
                if (maybeModule.isPresent() && this.neighborState.remove(moduleKey, removedNeighbor)) {
                    this.configModuleOp.removeModuleConfiguration(moduleKey, rwTx);
                }
            } catch (ReadFailedException | TransactionCommitFailedException e) {
                LOG.error("Failed to remove a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        }
    }

    public void onNeighborModified(final Neighbor modifiedNeighbor) {
        final ModuleKey moduleKey = this.neighborState.getModuleKey(modifiedNeighbor.getKey());
        final ReadOnlyTransaction rTx = this.dataBroker.newReadOnlyTransaction();
        final List<AdvertizedTable> advertizedTables = getAdvertizedTables(modifiedNeighbor, rTx);
        final List<AddPath> addPathCapabilities = getAddPathCapabilities(modifiedNeighbor, rTx);
        if (moduleKey != null) {
            updateExistingPeerConfiguration(moduleKey, modifiedNeighbor, advertizedTables, rTx, addPathCapabilities);
        } else {
            createNewPeerConfiguration(moduleKey, modifiedNeighbor, advertizedTables, rTx, addPathCapabilities);
        }
    }

    private List<AdvertizedTable> getAdvertizedTables(final Neighbor modifiedNeighbor, final ReadOnlyTransaction rTx) {
        return TableTypesFunction.getLocalTables(rTx, this.configModuleOp, ADVERTIZED_TABLE_FUNCTION, modifiedNeighbor.getAfiSafis().getAfiSafi());
    }

    private List<AddPath> getAddPathCapabilities(final Neighbor modifiedNeighbor, final ReadOnlyTransaction rTx) {
        return AddPathFunction.getAddPath(rTx, this.configModuleOp, ADD_PATH_FUNCTION, modifiedNeighbor.getAfiSafis().getAfiSafi());
    }

    private void updateExistingPeerConfiguration(final ModuleKey moduleKey, final Neighbor modifiedNeighbor, final List<AdvertizedTable>
        advertizedTables, final ReadOnlyTransaction rTx, final List<AddPath> addPathCapabilities) {
        if (neighborState.addOrUpdate(moduleKey, modifiedNeighbor.getKey(), modifiedNeighbor)) {
            final Optional<Module> maybeModule = getOldModuleConfiguration(moduleKey, rTx);
            if (maybeModule.isPresent()) {
                final Module peerConfigModule = toPeerConfigModule(modifiedNeighbor, maybeModule.get(), advertizedTables, addPathCapabilities);
                putOldModuleConfigurationIntoNewModule(peerConfigModule);
            }
        }
    }

    private Optional<Module> getOldModuleConfiguration(final ModuleKey moduleKey, final ReadOnlyTransaction rTx) {
        try {
            return this.configModuleOp.readModuleConfiguration(moduleKey, rTx);
        } catch (final Exception e) {
            LOG.error("Failed to read module configuration: {}", moduleKey, e);
            throw new IllegalStateException(e);
        }
    }

    private void putOldModuleConfigurationIntoNewModule(final Module peerConfigModule) {
        try {
            this.configModuleOp.putModuleConfiguration(peerConfigModule, this.dataBroker.newWriteOnlyTransaction());
        } catch (final TransactionCommitFailedException e) {
            LOG.error("Failed to update a configuration module: {}", peerConfigModule, e);
            throw new IllegalStateException(e);
        }
    }

    private void createNewPeerConfiguration(final ModuleKey moduleKey, final Neighbor modifiedNeighbor, final List<AdvertizedTable> advertizedTables,
            final ReadOnlyTransaction rTx, final List<AddPath> addPathCapabilities) {
        final ModuleKey ribImplKey = this.globalState.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER);
        if (ribImplKey != null) {
            try {
                final Rib rib = RibInstanceFunction.getRibInstance(this.configModuleOp, TO_RIB_FUNCTION, ribImplKey.getName(), rTx);
                final RpcRegistry rpcReg = RpcRegistryFunction.getRpcRegistryInstance(rTx, this.configModuleOp, RPC_REG_FUNCTION);
                final Module peerConfigModule = toPeerConfigModule(modifiedNeighbor, advertizedTables, rib, rpcReg, addPathCapabilities);
                this.configModuleOp.putModuleConfiguration(peerConfigModule, this.dataBroker.newWriteOnlyTransaction());
                this.neighborState.addOrUpdate(peerConfigModule.getKey(), modifiedNeighbor.getKey(), modifiedNeighbor);
            } catch (final Exception e) {
                LOG.error("Failed to create a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        }
    }

    private static Module toPeerConfigModule(final Neighbor neighbor, final Module oldBgpPeer, final List<AdvertizedTable> tableTypes,
            final List<AddPath> addPathCapabilities) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpPeer bgpPeer =
            (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpPeer) oldBgpPeer.getConfiguration();
        final BgpPeerBuilder bgpPeerBuilder = toBgpPeerConfig(neighbor, tableTypes, bgpPeer.getRib(), bgpPeer.getRpcRegistry(), addPathCapabilities);
        bgpPeerBuilder.setPeerRegistry(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpPeer) oldBgpPeer.getConfiguration()).getPeerRegistry());
        bgpPeerBuilder.setPort(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev160330.modules.module.configuration.BgpPeer) oldBgpPeer.getConfiguration()).getPort());

        final ModuleBuilder mBuilder = new ModuleBuilder(oldBgpPeer);
        mBuilder.setConfiguration(bgpPeerBuilder.build());
        return mBuilder.build();
    }

    private static Module toPeerConfigModule(final Neighbor neighbor, final List<AdvertizedTable> tableTypes, final Rib rib, final RpcRegistry rpcReg,
            final List<AddPath> addPathCapabilities) {
        final ModuleBuilder mBuilder = new ModuleBuilder();
        mBuilder.setName(createPeerName(neighbor.getNeighborAddress()));
        mBuilder.setType(BgpPeer.class);
        mBuilder.setConfiguration(toBgpPeerConfig(neighbor, tableTypes, rib, rpcReg, addPathCapabilities).build());
        mBuilder.setKey(new ModuleKey(mBuilder.getName(), mBuilder.getType()));
        return mBuilder.build();
    }

    private static BgpPeerBuilder toBgpPeerConfig(final Neighbor neighbor, final List<AdvertizedTable> tableTypes, final Rib rib, final RpcRegistry rpcReg,
            final List<AddPath> addPathCapabilities) {
        final BgpPeerBuilder bgpPeerBuilder = new BgpPeerBuilder();
        if (rpcReg != null) {
            bgpPeerBuilder.setRpcRegistry(rpcReg);
        }
        bgpPeerBuilder.setAdvertizedTable(tableTypes);
        bgpPeerBuilder.setAddPath(addPathCapabilities);
        bgpPeerBuilder.setRib(rib);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress ipAdress = null;
        if (neighbor.getNeighborAddress().getIpv4Address() != null) {
            ipAdress = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress(new org.opendaylight.yang
                .gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address(neighbor.getNeighborAddress().getIpv4Address().getValue()));
        } else {
            ipAdress = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress(new org.opendaylight.yang
                .gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address(neighbor.getNeighborAddress().getIpv6Address().getValue()));
        }
        bgpPeerBuilder.setHost(ipAdress);
        final Timers timers = neighbor.getTimers();
        if (timers != null && timers.getConfig() != null && timers.getConfig().getHoldTime() != null) {
            bgpPeerBuilder.setHoldtimer(neighbor.getTimers().getConfig().getHoldTime().intValue());
        }
        final Transport transport = neighbor.getTransport();
        if (transport != null && transport.getConfig() != null && transport.getConfig().isPassiveMode() != null) {
            bgpPeerBuilder.setInitiateConnection(!neighbor.getTransport().getConfig().isPassiveMode());
        }
        if (neighbor.getConfig() != null) {
            if (neighbor.getConfig().getAuthPassword() != null) {
                bgpPeerBuilder.setPassword(new Rfc2385Key(neighbor.getConfig().getAuthPassword()));
            }
            if (neighbor.getConfig().getPeerAs() != null) {
                bgpPeerBuilder.setRemoteAs(neighbor.getConfig().getPeerAs().getValue());
            }
            if (neighbor.getConfig().getPeerType() != null) {
                bgpPeerBuilder.setPeerRole(toPeerRole(neighbor));
            }
        }
        return bgpPeerBuilder;
    }

    private static String createPeerName(final IpAddress ipAddress) {
        final String address = ipAddress.getIpv4Address() != null ? ipAddress.getIpv4Address().getValue() : ipAddress.getIpv6Address().getValue();
        return PEER + address;
    }

    private static PeerRole toPeerRole(final Neighbor neighbor) {
        if (isRrClient(neighbor)) {
            return PeerRole.RrClient;
        }

        if (neighbor.getConfig() != null) {
            final PeerType peerType = neighbor.getConfig().getPeerType();
            if (peerType == PeerType.INTERNAL) {
                return PeerRole.Ibgp;
            }
            if (peerType == PeerType.EXTERNAL) {
                return PeerRole.Ebgp;
            }
        }
        LOG.info("Unknown peer role, setting peer {} role to iBGP", neighbor.getKey());
        return PeerRole.Ibgp;
    }

    private static boolean isRrClient(final Neighbor neighbor) {
        final RouteReflector routeReflector = neighbor.getRouteReflector();
        if (routeReflector != null && routeReflector.getConfig() != null) {
            return routeReflector.getConfig().isRouteReflectorClient();
        }
        return false;
    }

}
