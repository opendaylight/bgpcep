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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer.WriteConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppPeer implements PeerBean, BGPPeerStateConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AppPeer.class);
    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();
    private Neighbor currentConfiguration;
    private BgpAppPeerSingletonService bgpAppPeerSingletonService;
    private ServiceRegistration<?> serviceRegistration;

    @Override
    public void start(final RIB rib, final Neighbor neighbor, final BGPTableTypeRegistryConsumer tableTypeRegistry,
        final WriteConfiguration configurationWriter) {
        Preconditions.checkState(this.bgpAppPeerSingletonService == null, "Previous peer instance was not closed.");
        this.currentConfiguration = neighbor;
        this.bgpAppPeerSingletonService = new BgpAppPeerSingletonService(rib, createAppRibId(neighbor),
            neighbor.getNeighborAddress().getIpv4Address(), configurationWriter);
    }

    @Override
    public void restart(final RIB rib, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.currentConfiguration != null);
        start(rib, this.currentConfiguration, tableTypeRegistry, null);
    }

    @Override
    public ListenableFuture<Void> close() {
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }

        if (this.bgpAppPeerSingletonService == null) {
            return Futures.immediateFuture(null);
        }
        final ListenableFuture<Void> peerClose = this.bgpAppPeerSingletonService.closeServiceInstance();
        this.bgpAppPeerSingletonService = null;
        return peerClose;
    }

    @Override
    public Boolean containsEqualConfiguration(final Neighbor neighbor) {
        return Objects.equals(this.currentConfiguration.getKey(), neighbor.getKey())
                && OpenConfigMappingUtil.isApplicationPeer(neighbor);
    }

    @Override
    public void instantiateServiceInstance() {
        this.bgpAppPeerSingletonService.instantiateServiceInstance();
    }


    private static ApplicationRibId createAppRibId(final Neighbor neighbor) {
        final Config config = neighbor.getConfig();
        if (config != null && !Strings.isNullOrEmpty(config.getDescription())) {
            return new ApplicationRibId(config.getDescription());
        }
        return new ApplicationRibId(neighbor.getNeighborAddress().getIpv4Address().getValue());
    }

    @Override
    public BGPPeerState getPeerState() {
        return this.bgpAppPeerSingletonService.getPeerState();
    }

    void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    private final class BgpAppPeerSingletonService implements BGPPeerStateConsumer {
        private final ApplicationPeer applicationPeer;
        private final DOMDataTreeChangeService dataTreeChangeService;
        private final ApplicationRibId appRibId;
        private final WriteConfiguration configurationWriter;

        BgpAppPeerSingletonService(final RIB rib, final ApplicationRibId appRibId, final Ipv4Address neighborAddress,
            final WriteConfiguration configurationWriter) {
            this.applicationPeer = new ApplicationPeer(appRibId, neighborAddress, rib);
            this.appRibId = appRibId;
            this.dataTreeChangeService = rib.getService();
            this.configurationWriter = configurationWriter;
            LOG.info("Application Peer service {} created", appRibId.getValue());
        }

        public void instantiateServiceInstance() {
            if(this.configurationWriter != null) {
                this.configurationWriter.apply();
            }
            LOG.info("Application Peer {} service instantiated ", this.appRibId.getValue());
            final YangInstanceIdentifier yangIId = YangInstanceIdentifier.builder().node(ApplicationRib.QNAME)
                .nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, this.appRibId.getValue()).node(Tables.QNAME).node(Tables.QNAME).build();
            this.applicationPeer.instantiateServiceInstance(this.dataTreeChangeService,
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yangIId));
        }

        public ListenableFuture<Void> closeServiceInstance() {
            LOG.info("Application Peer {} close service instance ", this.appRibId.getValue());
            return this.applicationPeer.close();
        }

        @Override
        public BGPPeerState getPeerState() {
            return this.applicationPeer.getPeerState();
        }
    }
}