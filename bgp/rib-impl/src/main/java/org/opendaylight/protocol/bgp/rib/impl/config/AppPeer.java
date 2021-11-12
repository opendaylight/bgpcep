/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppPeer implements PeerBean, BGPPeerStateProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AppPeer.class);
    private static final NodeIdentifier APPRIB = NodeIdentifier.create(ApplicationRib.QNAME);
    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();
    private final BGPStateProviderRegistry stateProviderRegistry;
    @GuardedBy("this")
    private Neighbor currentConfiguration;
    @GuardedBy("this")
    private BgpAppPeerSingletonService bgpAppPeerSingletonService;
    private Registration stateProviderRegistration;

    public AppPeer(final BGPStateProviderRegistry stateProviderRegistry) {
        this.stateProviderRegistry = requireNonNull(stateProviderRegistry);
    }

    private static ApplicationRibId createAppRibId(final Neighbor neighbor) {
        final Config config = neighbor.getConfig();
        if (config != null && !Strings.isNullOrEmpty(config.getDescription())) {
            return new ApplicationRibId(config.getDescription());
        }
        return new ApplicationRibId(neighbor.getNeighborAddress().getIpv4Address().getValue());
    }

    @Override
    public synchronized void start(final RIB rib, final Neighbor neighbor, final InstanceIdentifier<Bgp> bgpIid,
            final PeerGroupConfigLoader peerGroupLoader, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(bgpAppPeerSingletonService == null,
                "Previous peer instance was not closed.");
        LOG.info("Starting AppPeer instance {}", neighbor.getNeighborAddress());
        currentConfiguration = neighbor;
        bgpAppPeerSingletonService = new BgpAppPeerSingletonService(rib, createAppRibId(neighbor),
            IetfInetUtil.INSTANCE.ipv4AddressNoZoneFor(neighbor.getNeighborAddress().getIpv4Address()),
            tableTypeRegistry);
        stateProviderRegistration = stateProviderRegistry.register(this);
    }

    @Override
    public synchronized void close() throws ExecutionException, InterruptedException {
        if (bgpAppPeerSingletonService == null) {
            LOG.info("App Peer {} already closed, skipping", currentConfiguration.getNeighborAddress());
            return;
        }
        LOG.info("Closing App Peer {}", currentConfiguration.getNeighborAddress());
        if (stateProviderRegistration != null) {
            stateProviderRegistration.close();
            stateProviderRegistration = null;
        }
        closeServiceInstance().get();
        bgpAppPeerSingletonService = null;
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        if (bgpAppPeerSingletonService != null) {
            bgpAppPeerSingletonService.instantiateServiceInstance();
        }
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        if (bgpAppPeerSingletonService != null) {
            return bgpAppPeerSingletonService.closeServiceInstance();
        }

        return CommitInfo.emptyFluentFuture();
    }

    @Override
    public synchronized Boolean containsEqualConfiguration(final Neighbor neighbor) {
        return Objects.equals(currentConfiguration.key(), neighbor.key())
                && OpenConfigMappingUtil.isApplicationPeer(neighbor);
    }

    @Override
    public synchronized Neighbor getCurrentConfiguration() {
        return currentConfiguration;
    }

    @Override
    public synchronized BGPPeerState getPeerState() {
        return bgpAppPeerSingletonService.getPeerState();
    }

    private static final class BgpAppPeerSingletonService implements BGPPeerStateProvider {
        private final ApplicationPeer applicationPeer;
        private final DOMDataTreeChangeService dataTreeChangeService;
        private final ApplicationRibId appRibId;
        @GuardedBy("this")
        private boolean isServiceInstantiated;

        BgpAppPeerSingletonService(final RIB rib, final ApplicationRibId appRibId,
                final Ipv4AddressNoZone neighborAddress, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
            applicationPeer = new ApplicationPeer(tableTypeRegistry, appRibId, neighborAddress, rib);
            this.appRibId = appRibId;
            dataTreeChangeService = rib.getService();
        }

        public synchronized void instantiateServiceInstance() {
            isServiceInstantiated = true;
            final YangInstanceIdentifier yangIId = YangInstanceIdentifier.builder().node(APPRIB)
                    .nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, appRibId.getValue())
                    .node(TABLES_NID).node(TABLES_NID).build();
            applicationPeer.instantiateServiceInstance(dataTreeChangeService,
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yangIId));
        }

        public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
            if (!isServiceInstantiated) {
                LOG.trace("Application peer already closed {}", appRibId.getValue());
                return CommitInfo.emptyFluentFuture();
            }
            LOG.info("Application peer instance closed {}", appRibId.getValue());
            isServiceInstantiated = false;
            return applicationPeer.close();
        }

        @Override
        public BGPPeerState getPeerState() {
            return applicationPeer.getPeerState();
        }
    }
}
