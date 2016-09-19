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
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer.WriteConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppPeer implements PeerBean {
    private static final Logger LOG = LoggerFactory.getLogger(AppPeer.class);
    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();
    private Neighbor currentConfiguration;
    private BgpAppPeerSingletonService bgpAppPeerSingletonService;
    private BGPOpenConfigMappingService mappingService;

    @Override
    public void start(final RIB rib, final Neighbor neighbor, final BGPOpenConfigMappingService mappingService, final WriteConfiguration configurationWriter) {
        this.currentConfiguration = neighbor;
        this.mappingService = mappingService;
        this.bgpAppPeerSingletonService = new BgpAppPeerSingletonService(rib, createAppRibId(neighbor), neighbor.getNeighborAddress().getIpv4Address(),
                configurationWriter);
    }

    @Override
    public void restart(final RIB rib, final BGPOpenConfigMappingService mappingService) {
        Preconditions.checkState(this.currentConfiguration != null);
        start(rib, this.currentConfiguration, mappingService, null);
    }

    @Override
    public void close() {
        try {
            this.bgpAppPeerSingletonService.close();
        } catch (final Exception e) {
            LOG.warn("Failed to close application peer instance", e);
        }
    }

    @Override
    public Boolean containsEqualConfiguration(final Neighbor neighbor) {
        return Objects.equals(this.currentConfiguration.getKey(), neighbor.getKey())
                && this.mappingService.isApplicationPeer(neighbor);
    }

    private static ApplicationRibId createAppRibId(final Neighbor neighbor) {
        final Config config = neighbor.getConfig();
        if (config != null && !Strings.isNullOrEmpty(config.getDescription())) {
            return new ApplicationRibId(config.getDescription());
        }
        return new ApplicationRibId(neighbor.getNeighborAddress().getIpv4Address().getValue());
    }

    private final class BgpAppPeerSingletonService implements ClusterSingletonService, AutoCloseable {
        private final ApplicationPeer applicationPeer;
        private final DOMDataTreeChangeService dataTreeChangeService;
        private final ApplicationRibId appRibId;
        private ClusterSingletonServiceRegistration singletonServiceRegistration;
        private ListenerRegistration<ApplicationPeer> registration;
        private final ServiceGroupIdentifier serviceGroupIdentifier;
        private final WriteConfiguration configurationWriter;

        BgpAppPeerSingletonService(final RIB rib, final ApplicationRibId appRibId, final Ipv4Address neighborAddress, final WriteConfiguration configurationWriter) {
            this.applicationPeer = new ApplicationPeer(appRibId, neighborAddress, rib);
            this.appRibId = appRibId;
            this.dataTreeChangeService = rib.getService();
            this.serviceGroupIdentifier = rib.getRibIServiceGroupIdentifier();
            this.configurationWriter = configurationWriter;
            LOG.info("Application Peer Singleton Service {} registered", getIdentifier());
            //this need to be always the last step
            this.singletonServiceRegistration = rib.registerClusterSingletonService(this);
        }

        @Override
        public void close() throws Exception {
            if (this.singletonServiceRegistration != null) {
                this.singletonServiceRegistration.close();
                this.singletonServiceRegistration = null;
            }
        }

        @Override
        public void instantiateServiceInstance() {
            if(this.configurationWriter != null) {
                this.configurationWriter.apply();
            }
            LOG.info("Application Peer Singleton Service {} instantiated", getIdentifier());
            final YangInstanceIdentifier yangIId = YangInstanceIdentifier.builder().node(ApplicationRib.QNAME)
                .nodeWithKey(ApplicationRib.QNAME, APP_ID_QNAME, this.appRibId.getValue()).node(Tables.QNAME).node(Tables.QNAME).build();
            this.applicationPeer.instantiateServiceInstance();
            this.registration = this.dataTreeChangeService
                .registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yangIId), this.applicationPeer);
        }

        @Override
        public ListenableFuture<Void> closeServiceInstance() {
            LOG.info("Application Peer Singleton Service {} instance closed", getIdentifier());
            if(this.registration != null) {
                this.registration.close();
            }
            this.applicationPeer.close();
            return Futures.immediateFuture(null);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return this.serviceGroupIdentifier;
        }
    }
}