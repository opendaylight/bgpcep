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
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer.WriteConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppPeer implements PeerBean, BGPPeerState {
    private static final Logger LOG = LoggerFactory.getLogger(AppPeer.class);
    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();
    private Neighbor currentConfiguration;
    private BgpAppPeerSingletonService bgpAppPeerSingletonService;

    @Override
    public void start(final RIB rib, final Neighbor neighbor, final BGPTableTypeRegistryConsumer tableTypeRegistry,
        final WriteConfiguration configurationWriter) {
        Preconditions.checkState(this.bgpAppPeerSingletonService == null, "Previous peer instance was not closed.");
        this.currentConfiguration = neighbor;
        this.bgpAppPeerSingletonService = new BgpAppPeerSingletonService(rib, createAppRibId(neighbor),
            neighbor.getNeighborAddress(), configurationWriter);
    }

    @Override
    public void restart(final RIB rib, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.currentConfiguration != null);
        start(rib, this.currentConfiguration, tableTypeRegistry, null);
    }

    @Override
    public void close() {
        try {
            this.bgpAppPeerSingletonService.close();
            this.bgpAppPeerSingletonService = null;
        } catch (final Exception e) {
            LOG.warn("Failed to close application peer instance", e);
        }
    }

    @Override
    public Boolean containsEqualConfiguration(final Neighbor neighbor) {
        return Objects.equals(this.currentConfiguration.getKey(), neighbor.getKey())
                && OpenConfigMappingUtil.isApplicationPeer(neighbor);
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
        private final ServiceGroupIdentifier serviceGroupIdentifier;
        private final WriteConfiguration configurationWriter;

        BgpAppPeerSingletonService(final RIB rib, final ApplicationRibId appRibId, final IpAddress neighborAddress,
            final WriteConfiguration configurationWriter) {
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
            this.applicationPeer.instantiateServiceInstance(this.dataTreeChangeService,
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yangIId));
        }

        @Override
        public ListenableFuture<Void> closeServiceInstance() {
            LOG.info("Application Peer Singleton Service {} instance closed", getIdentifier());
            this.applicationPeer.close();
            return Futures.immediateFuture(null);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return this.serviceGroupIdentifier;
        }

        BGPPeerState getPeer() {
            return this.applicationPeer;
        }
    }

    @Override
    public IpAddress getNeighborAddress() {
        return this.bgpAppPeerSingletonService.getPeer().getNeighborAddress();
    }

    @Override
    public long getTotalPrefixes() {
        return this.bgpAppPeerSingletonService.getPeer().getTotalPrefixes();
    }

    @Override
    public long getPrefixesSentCount(@Nonnull final TablesKey tablesKey) {
        return this.bgpAppPeerSingletonService.getPeer().getPrefixesSentCount(tablesKey);
    }

    @Override
    public long getPrefixesReceivedCount(@Nonnull final TablesKey tablesKey) {
        return this.bgpAppPeerSingletonService.getPeer().getPrefixesReceivedCount(tablesKey);
    }

    @Override
    public long getErroneousUpdateReceivedCount() {
        return this.bgpAppPeerSingletonService.getPeer().getErroneousUpdateReceivedCount();
    }

    @Override
    public long getUpdateMessagesSentCount() {
        return this.bgpAppPeerSingletonService.getPeer().getUpdateMessagesSentCount();
    }

    @Override
    public long getNotificationMessagesSentCount() {
        return this.bgpAppPeerSingletonService.getPeer().getNotificationMessagesSentCount();
    }

    @Override
    public long getUpdateMessagesReceivedCount() {
        return this.bgpAppPeerSingletonService.getPeer().getUpdateMessagesReceivedCount();
    }

    @Override
    public long getNotificationMessagesReceivedCount() {
        return this.bgpAppPeerSingletonService.getPeer().getNotificationMessagesReceivedCount();
    }

    @Override
    public boolean isAddPathCapabilitySupported() {
        return this.bgpAppPeerSingletonService.getPeer().isAddPathCapabilitySupported();
    }

    @Override
    public boolean isAsn32CapabilitySupported() {
        return this.bgpAppPeerSingletonService.getPeer().isAsn32CapabilitySupported();
    }

    @Override
    public boolean isGracefulRestartCapabilitySupported() {
        return this.bgpAppPeerSingletonService.getPeer().isGracefulRestartCapabilitySupported();
    }

    @Override
    public boolean isMultiProtocolCapabilitySupported() {
        return this.bgpAppPeerSingletonService.getPeer().isMultiProtocolCapabilitySupported();
    }

    @Override
    public boolean isRouterRefreshCapabilitySupported() {
        return this.bgpAppPeerSingletonService.getPeer().isRouterRefreshCapabilitySupported();
    }

    @Override
    public State getSessionState() {
        return this.bgpAppPeerSingletonService.getPeer().getSessionState();
    }

    @Nonnull
    @Override
    public Set<TablesKey> getAfiSafisAdvertized() {
        return this.bgpAppPeerSingletonService.getPeer().getAfiSafisAdvertized();
    }

    @Nonnull
    @Override
    public Set<TablesKey> getAfiSafisReceived() {
        return this.bgpAppPeerSingletonService.getPeer().getAfiSafisReceived();
    }

    @Override
    public long getPrefixesInstalledCount(@Nonnull final TablesKey tablesKey) {
        return this.bgpAppPeerSingletonService.getPeer().getPrefixesInstalledCount(tablesKey);
    }

    @Override
    public boolean isAfiSafiSupported(@Nonnull final TablesKey tablesKey) {
        return this.bgpAppPeerSingletonService.getPeer().isAfiSafiSupported(tablesKey);
    }

    @Override
    public boolean isGracefulRestartAdvertized(@Nonnull final TablesKey tablesKey) {
        return this.bgpAppPeerSingletonService.getPeer().isGracefulRestartAdvertized(tablesKey);
    }

    @Override
    public boolean isGracefulRestartReceived(final TablesKey tablesKey) {
        return this.bgpAppPeerSingletonService.getPeer().isGracefulRestartReceived(tablesKey);
    }

    @Override
    public long getNegotiatedHoldTime() {
        return this.bgpAppPeerSingletonService.getPeer().getNegotiatedHoldTime();
    }

    @Override
    public long getUpTime() {
        return this.bgpAppPeerSingletonService.getPeer().getUpTime();
    }

    @Nonnull
    @Override
    public PortNumber getLocalPort() {
        return this.bgpAppPeerSingletonService.getPeer().getLocalPort();
    }

    @Nonnull
    @Override
    public IpAddress getRemoteAddress() {
        return this.bgpAppPeerSingletonService.getPeer().getRemoteAddress();
    }

    @Nonnull
    @Override
    public PortNumber getRemotePort() {
        return this.bgpAppPeerSingletonService.getPeer().getRemotePort();
    }

    @Override
    public boolean isLocalRestarting() {
        return this.bgpAppPeerSingletonService.getPeer().isLocalRestarting();
    }

    @Override
    public int isPeerRestartTime() {
        return this.bgpAppPeerSingletonService.getPeer().isPeerRestartTime();
    }

    @Override
    public boolean isPeerRestarting() {
        return this.bgpAppPeerSingletonService.getPeer().isPeerRestarting();
    }
}