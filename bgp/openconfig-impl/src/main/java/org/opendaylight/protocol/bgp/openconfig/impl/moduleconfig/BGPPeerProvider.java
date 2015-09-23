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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.RouteReflector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.PeerType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.AdvertizedTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.AdvertizedTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.peer.RibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
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

    private final BGPConfigHolder<Neighbor> neighborState;
    private final BGPConfigHolder<Global> globalState;
    private final BGPConfigModuleProvider configModuleOp;

    public BGPPeerProvider(final BGPConfigStateStore configHolders, final BGPConfigModuleProvider configModuleWriter) {
        this.configModuleOp = Preconditions.checkNotNull(configModuleWriter);
        this.globalState = Preconditions.checkNotNull(configHolders.getBGPConfigHolder(Global.class));
        this.neighborState = Preconditions.checkNotNull(configHolders.getBGPConfigHolder(Neighbor.class));
    }

    public void onNeighborRemoved(final Neighbor removedNeighbor, final DataBroker dataBroker) {
        final ModuleKey moduleKey = neighborState.getModuleKey(removedNeighbor.getKey());
        if (moduleKey != null) {
            try {
                globalState.remove(moduleKey);
                final Optional<Module> maybeModule = configModuleOp.readModuleConfiguration(moduleKey, dataBroker.newReadOnlyTransaction()).get();
                if (maybeModule.isPresent()) {
                    configModuleOp.removeModuleConfiguration(moduleKey, dataBroker.newWriteOnlyTransaction());
                }
            } catch (InterruptedException | ExecutionException | TransactionCommitFailedException e) {
                LOG.error("Failed to remove a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        }
    }

    public void onNeighborModified(final Neighbor modifiedNeighbor, final DataBroker dataBroker) {
        final ModuleKey moduleKey = neighborState.getModuleKey(modifiedNeighbor.getKey());
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        final ListenableFuture<List<AdvertizedTable>> advertizedTablesFuture = new TableTypesFunction<AdvertizedTable>(rTx,
                configModuleOp, ADVERTIZED_TABLE_FUNCTION).apply(modifiedNeighbor.getAfiSafis().getAfiSafi());
        if (moduleKey != null) {
            //update an existing peer configuration
            try {
                if (neighborState.addOrUpdate(moduleKey, modifiedNeighbor.getKey(), modifiedNeighbor)) {
                    final Optional<Module> maybeModule = configModuleOp.readModuleConfiguration(moduleKey, rTx).get();
                    if (maybeModule.isPresent()) {
                        final Module peerConfigModule = toPeerConfigModule(modifiedNeighbor, maybeModule.get(), advertizedTablesFuture.get());
                        configModuleOp.putModuleConfiguration(peerConfigModule, dataBroker.newWriteOnlyTransaction());
                    }
                }
            } catch (final Exception e) {
                LOG.error("Failed to update a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        } else {
            //create new peer configuration
            final ModuleKey ribImplKey = globalState.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER);
            if (ribImplKey != null) {
                try {
                    final ListenableFuture<Rib> ribFuture = new RibInstanceFunction<>(rTx, configModuleOp, TO_RIB_FUNCTION).apply(ribImplKey.getName());
                    final Module peerConfigModule = toPeerConfigModule(modifiedNeighbor, advertizedTablesFuture.get(), ribFuture.get());
                    configModuleOp.putModuleConfiguration(peerConfigModule, dataBroker.newWriteOnlyTransaction());
                    neighborState.addOrUpdate(peerConfigModule.getKey(), modifiedNeighbor.getKey(), modifiedNeighbor);
                } catch (final Exception e) {
                    LOG.error("Failed to create a configuration module: {}", moduleKey, e);
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private static Module toPeerConfigModule(final Neighbor neighbor, final Module oldBgpPeer, final List<AdvertizedTable> tableTypes) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpPeer bgpPeer =
                (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpPeer) oldBgpPeer.getConfiguration();
        final BgpPeerBuilder bgpPeerBuilder = toBgpPeerConfig(neighbor, tableTypes, bgpPeer.getRib());
        bgpPeerBuilder.setPeerRegistry(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpPeer)oldBgpPeer.getConfiguration()).getPeerRegistry());
        bgpPeerBuilder.setPort(((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpPeer)oldBgpPeer.getConfiguration()).getPort());

        final ModuleBuilder mBuilder = new ModuleBuilder(oldBgpPeer);
        mBuilder.setConfiguration(bgpPeerBuilder.build());
        return mBuilder.build();
    }

    private static Module toPeerConfigModule(final Neighbor neighbor, final List<AdvertizedTable> tableTypes, final Rib rib) {
        final ModuleBuilder mBuilder = new ModuleBuilder();
        mBuilder.setName(createPeerName(neighbor.getNeighborAddress()));
        mBuilder.setType(BgpPeer.class);
        mBuilder.setConfiguration(toBgpPeerConfig(neighbor, tableTypes, rib).build());
        mBuilder.setKey(new ModuleKey(mBuilder.getName(), mBuilder.getType()));
        return mBuilder.build();
    }

    private static BgpPeerBuilder toBgpPeerConfig(final Neighbor neighbor, final List<AdvertizedTable> tableTypes, final Rib rib) {
        final BgpPeerBuilder bgpPeerBuilder = new BgpPeerBuilder();
        bgpPeerBuilder.setAdvertizedTable(tableTypes);
        bgpPeerBuilder.setRib(rib);
        bgpPeerBuilder.setHost(neighbor.getNeighborAddress());
        final Timers timers = neighbor.getTimers();
        if (timers != null && timers.getConfig() != null && timers.getConfig().getHoldTime() != null) {
            bgpPeerBuilder.setHoldtimer(neighbor.getTimers().getConfig().getHoldTime().shortValue());
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
