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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpApplicationPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.BgpApplicationPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.application.peer.TargetRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.bgp.application.peer.TargetRibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPAppPeerProvider {

    private static final String APPLICATION_RIB = "application-rib_";

    private static final String APPLICATION_PEER = "application-peer_";

    private static final Logger LOG = LoggerFactory.getLogger(BGPAppPeerProvider.class);

    private static final Function<String, TargetRib> TO_RIB_FUNCTION = new Function<String, TargetRib>() {
        @Override
        public TargetRib apply(final String name) {
            return new TargetRibBuilder().setName(name).setType(RibInstance.class).build();
        }
    };

    private final BGPConfigHolder<Neighbor> neighborState;
    private final BGPConfigHolder<Global> globalState;
    private final BGPConfigModuleProvider configModuleOp;

    public BGPAppPeerProvider(final BGPConfigStateStore configHolders, final BGPConfigModuleProvider configModuleWriter) {
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
            } catch (final Exception e) {
                LOG.error("Failed to remove a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        }
    }

    public void onNeighborModified(final Neighbor modifiedAppNeighbor, final DataBroker dataBroker) {
        final ModuleKey moduleKey = neighborState.getModuleKey(modifiedAppNeighbor.getKey());
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        if (moduleKey != null) {
            //update an existing peer configuration
            try {
                if (neighborState.addOrUpdate(moduleKey, modifiedAppNeighbor.getKey(), modifiedAppNeighbor)) {
                    final Optional<Module> maybeModule = configModuleOp.readModuleConfiguration(moduleKey, rTx).get();
                    if (maybeModule.isPresent()) {
                        final Module peerConfigModule = toPeerConfigModule(modifiedAppNeighbor, maybeModule.get());
                        configModuleOp.putModuleConfiguration(peerConfigModule, dataBroker.newWriteOnlyTransaction());
                    }
                }
            } catch (final Exception e) {
                LOG.error("Failed to update a configuration module: {}", moduleKey, e);
                throw new IllegalStateException(e);
            }
        } else {
            //create new application peer configuration
            final ModuleKey ribImplKey = globalState.getModuleKey(GlobalIdentifier.GLOBAL_IDENTIFIER);
            if (ribImplKey != null) {
                try {
                    final ListenableFuture<TargetRib> ribFuture = new RibInstanceFunction<>(rTx, configModuleOp, TO_RIB_FUNCTION).apply(ribImplKey.getName());
                    final Module peerConfigModule = toPeerConfigModule(modifiedAppNeighbor, ribFuture.get());
                    configModuleOp.putModuleConfiguration(peerConfigModule, dataBroker.newWriteOnlyTransaction());
                    neighborState.addOrUpdate(peerConfigModule.getKey(), modifiedAppNeighbor.getKey(), modifiedAppNeighbor);
                } catch (final Exception e) {
                    LOG.error("Failed to create a configuration module: {}", moduleKey, e);
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private static Module toPeerConfigModule(final Neighbor neighbor, final Module currentModule) {
        final BgpApplicationPeer appPeerConfig = (BgpApplicationPeer) currentModule.getConfiguration();
        final BgpApplicationPeerBuilder bgpPeerConfigBuilder = toBgpPeerConfig(neighbor, appPeerConfig.getTargetRib());
        bgpPeerConfigBuilder.setApplicationRibId(appPeerConfig.getApplicationRibId());
        final ModuleBuilder mBuilder = new ModuleBuilder();
        mBuilder.setConfiguration(bgpPeerConfigBuilder.build());
        return mBuilder.build();
    }

    private static Module toPeerConfigModule(final Neighbor neighbor, final TargetRib rib) {
        final ModuleBuilder mBuilder = new ModuleBuilder();
        mBuilder.setName(createAppPeerName(neighbor.getNeighborAddress().getIpv4Address()));
        mBuilder.setType(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpApplicationPeer.class);
        mBuilder.setConfiguration(toBgpPeerConfig(neighbor, rib).build());
        mBuilder.setKey(new ModuleKey(mBuilder.getName(), mBuilder.getType()));
        return mBuilder.build();
    }

    private static BgpApplicationPeerBuilder toBgpPeerConfig(final Neighbor neighbor, final TargetRib rib) {
        final BgpApplicationPeerBuilder bgpAppPeerBuilder = new BgpApplicationPeerBuilder();
        bgpAppPeerBuilder.setTargetRib(rib);
        final Ipv4Address address = neighbor.getNeighborAddress().getIpv4Address();
        bgpAppPeerBuilder.setBgpPeerId(address);
        bgpAppPeerBuilder.setApplicationRibId(new ApplicationRibId(createAppRibName(address)));
        return bgpAppPeerBuilder;
    }

    private static String createAppPeerName(final Ipv4Address ipAddress) {
        return APPLICATION_PEER + ipAddress.getValue();
    }

    private static String createAppRibName(final Ipv4Address ipAddress) {
        return APPLICATION_RIB + ipAddress.getValue();
    }
}
