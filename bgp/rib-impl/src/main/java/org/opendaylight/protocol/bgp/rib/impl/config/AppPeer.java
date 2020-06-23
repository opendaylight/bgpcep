/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Objects;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppPeer implements PeerBean, BGPPeerStateConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AppPeer.class);
    private static final NodeIdentifier APPRIB = NodeIdentifier.create(ApplicationRib.QNAME);
    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();
    @GuardedBy("this")
    private Neighbor currentConfiguration;
    @GuardedBy("this")
    private BgpAppPeerSingletonService bgpAppPeerSingletonService;

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
        Preconditions.checkState(this.bgpAppPeerSingletonService == null,
                "Previous peer instance was not closed.");
        this.currentConfiguration = neighbor;
        this.bgpAppPeerSingletonService = new BgpAppPeerSingletonService(rib, createAppRibId(neighbor),
            IetfInetUtil.INSTANCE.ipv4AddressNoZoneFor(neighbor.getNeighborAddress().getIpv4Address()),
            tableTypeRegistry);
    }

    @Override
    public synchronized void restart(final RIB rib, final InstanceIdentifier<Bgp> bgpIid,
            final PeerGroupConfigLoader peerGroupLoader, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.currentConfiguration != null);
        start(rib, this.currentConfiguration, bgpIid, peerGroupLoader, tableTypeRegistry);
    }

    @Override
    public synchronized void close() {
        if (this.bgpAppPeerSingletonService != null) {
            this.bgpAppPeerSingletonService = null;
        }
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        if (this.bgpAppPeerSingletonService != null) {
            this.bgpAppPeerSingletonService.instantiateServiceInstance();
        }
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        if (this.bgpAppPeerSingletonService != null) {
            return this.bgpAppPeerSingletonService.closeServiceInstance();
        }

        return CommitInfo.emptyFluentFuture();
    }

    @Override
    public synchronized Boolean containsEqualConfiguration(final Neighbor neighbor) {
        return Objects.equals(this.currentConfiguration.key(), neighbor.key())
                && OpenConfigMappingUtil.isApplicationPeer(neighbor);
    }

    @Override
    public synchronized BGPPeerState getPeerState() {
        return this.bgpAppPeerSingletonService.getPeerState();
    }

    private static final class BgpAppPeerSingletonService implements BGPPeerStateConsumer {
        private final ApplicationPeer applicationPeer;
        private final DOMDataTreeChangeService dataTreeChangeService;
        private final ApplicationRibId appRibId;
        @GuardedBy("this")
        private boolean isServiceInstantiated;

        BgpAppPeerSingletonService(final RIB rib, final ApplicationRibId appRibId,
                final Ipv4AddressNoZone neighborAddress, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
            this.applicationPeer = new ApplicationPeer(tableTypeRegistry, appRibId, neighborAddress, rib);
            this.appRibId = appRibId;
            this.dataTreeChangeService = rib.getService();
        }

        public synchronized void instantiateServiceInstance() {
            this.isServiceInstantiated = true;
            final YangInstanceIdentifier yangIId = YangInstanceIdentifier.builder().node(APPRIB)
                    .nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, this.appRibId.getValue())
                    .node(TABLES_NID).node(TABLES_NID).build();
            this.applicationPeer.instantiateServiceInstance(this.dataTreeChangeService,
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yangIId));
        }

        public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
            if (!this.isServiceInstantiated) {
                LOG.trace("Application peer already closed {}", this.appRibId.getValue());
                return CommitInfo.emptyFluentFuture();
            }
            LOG.info("Application peer instance closed {}", this.appRibId.getValue());
            this.isServiceInstantiated = false;
            return this.applicationPeer.close();
        }

        @Override
        public BGPPeerState getPeerState() {
            return this.applicationPeer.getPeerState();
        }
    }
}
