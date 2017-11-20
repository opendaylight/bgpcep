/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Objects;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppPeer implements PeerBean, BGPPeerStateConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AppPeer.class);
    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();
    @GuardedBy("this")
    private Neighbor currentConfiguration;
    @GuardedBy("this")
    private BgpAppPeerSingletonService bgpAppPeerSingletonService;
    @GuardedBy("this")
    private ServiceRegistration<?> serviceRegistration;

    private static ApplicationRibId createAppRibId(final Neighbor neighbor) {
        final Config config = neighbor.getConfig();
        if (config != null && !Strings.isNullOrEmpty(config.getDescription())) {
            return new ApplicationRibId(config.getDescription());
        }
        return new ApplicationRibId(neighbor.getNeighborAddress().getIpv4Address().getValue());
    }

    @Override
    public synchronized void start(final RIB rib, final Neighbor neighbor,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.bgpAppPeerSingletonService == null, "Previous peer instance was not closed.");
        this.currentConfiguration = neighbor;
        this.bgpAppPeerSingletonService = new BgpAppPeerSingletonService(rib, createAppRibId(neighbor),
                neighbor.getNeighborAddress().getIpv4Address());
    }

    @Override
    public synchronized void restart(final RIB rib, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.currentConfiguration != null);
        start(rib, this.currentConfiguration, tableTypeRegistry);
    }

    @Override
    public synchronized void close() {
        if (this.bgpAppPeerSingletonService != null) {
            this.bgpAppPeerSingletonService = null;
        }
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        if (this.bgpAppPeerSingletonService != null) {
            this.bgpAppPeerSingletonService.instantiateServiceInstance();
        }
    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        if (this.bgpAppPeerSingletonService != null) {
            return this.bgpAppPeerSingletonService.closeServiceInstance();
        }

        return Futures.immediateFuture(null);
    }

    @Override
    public Boolean containsEqualConfiguration(final Neighbor neighbor) {
        return Objects.equals(this.currentConfiguration.getKey(), neighbor.getKey())
                && OpenConfigMappingUtil.isApplicationPeer(neighbor);
    }

    @Override
    public BGPPeerState getPeerState() {
        return this.bgpAppPeerSingletonService.getPeerState();
    }

    synchronized void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    private final class BgpAppPeerSingletonService implements BGPPeerStateConsumer {
        private final ApplicationPeer applicationPeer;
        private final DOMDataTreeChangeService dataTreeChangeService;
        private final ApplicationRibId appRibId;
        @GuardedBy("this")
        private boolean isServiceInstantiated;

        BgpAppPeerSingletonService(final RIB rib, final ApplicationRibId appRibId, final Ipv4Address neighborAddress) {
            this.applicationPeer = new ApplicationPeer(appRibId, neighborAddress, rib);
            this.appRibId = appRibId;
            this.dataTreeChangeService = rib.getService();
        }

        public synchronized void instantiateServiceInstance() {
            this.isServiceInstantiated = true;
            final YangInstanceIdentifier yangIId = YangInstanceIdentifier.builder().node(ApplicationRib.QNAME)
                    .nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, this.appRibId.getValue())
                    .node(Tables.QNAME).node(Tables.QNAME).build();
            this.applicationPeer.instantiateServiceInstance(this.dataTreeChangeService,
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yangIId));
        }

        public synchronized ListenableFuture<Void> closeServiceInstance() {
            if (!this.isServiceInstantiated) {
                LOG.trace("Application peer already closed {}", this.appRibId.getValue());
                return Futures.immediateFuture(null);
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
