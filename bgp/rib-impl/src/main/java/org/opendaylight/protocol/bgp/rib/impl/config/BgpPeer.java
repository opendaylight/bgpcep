/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getHoldTimer;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getPeerAs;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getSimpleRoutingPolicy;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class BgpPeer implements PeerBean, BGPPeerStateConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(BgpPeer.class);

    private final RpcProviderRegistry rpcRegistry;
    @GuardedBy("this")
    private ServiceRegistration<?> serviceRegistration;
    @GuardedBy("this")
    private Neighbor currentConfiguration;
    @GuardedBy("this")
    private BgpPeerSingletonService bgpPeerSingletonService;

    public BgpPeer(final RpcProviderRegistry rpcRegistry) {
        this.rpcRegistry = rpcRegistry;
    }

    private static List<BgpParameters> getBgpParameters(final Neighbor neighbor, final RIB rib,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> caps = new ArrayList<>();
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().setAs4BytesCapability(
                new As4BytesCapabilityBuilder().setAsNumber(rib.getLocalAs()).build()).build()).build());

        caps.add(new OptionalCapabilitiesBuilder()
                .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        caps.add(new OptionalCapabilitiesBuilder()
                .setCParameters(MultiprotocolCapabilitiesUtil.RR_CAPABILITY).build());

        final List<AfiSafi> afiSafi = OpenConfigMappingUtil
                .getAfiSafiWithDefault(neighbor.getAfiSafis(), false);
        final List<AddressFamilies> addPathCapability = OpenConfigMappingUtil
                .toAddPathCapability(afiSafi, tableTypeRegistry);
        if (!addPathCapability.isEmpty()) {
            caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder()
                    .addAugmentation(CParameters1.class, new CParameters1Builder().setAddPathCapability(
                            new AddPathCapabilityBuilder().setAddressFamilies(addPathCapability)
                                    .build()).build()).build()).build());
        }

        final List<BgpTableType> tableTypes = OpenConfigMappingUtil.toTableTypes(afiSafi, tableTypeRegistry);
        for (final BgpTableType tableType : tableTypes) {
            if (!rib.getLocalTables().contains(tableType)) {
                LOG.info("RIB instance does not list {} in its local tables. Incoming data will be dropped.",
                        tableType);
            }

            caps.add(new OptionalCapabilitiesBuilder().setCParameters(
                    new CParametersBuilder().addAugmentation(CParameters1.class,
                            new CParameters1Builder().setMultiprotocolCapability(
                                    new MultiprotocolCapabilityBuilder(tableType).build()).build()).build()).build());
        }
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(caps).build());
        return tlvs;
    }

    private static Optional<byte[]> getPassword(final KeyMapping key) {
        if (key != null) {
            return Optional.of(Iterables.getOnlyElement(key.values()));
        }
        return Optional.absent();
    }

    @Override
    public synchronized void start(final RIB rib, final Neighbor neighbor,
            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.bgpPeerSingletonService == null,
                "Previous peer instance was not closed.");
        this.bgpPeerSingletonService = new BgpPeerSingletonService(rib, neighbor, tableTypeRegistry);
        this.currentConfiguration = neighbor;
    }

    @Override
    public synchronized void restart(final RIB rib, final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkState(this.currentConfiguration != null);
        start(rib, this.currentConfiguration, tableTypeRegistry);
    }

    @Override
    public synchronized void close() {
        closeSingletonService();
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        if (this.bgpPeerSingletonService != null) {
            return this.bgpPeerSingletonService.closeServiceInstance();
        }
        return Futures.immediateFuture(null);
    }

    private void closeSingletonService() {
        if (this.bgpPeerSingletonService != null) {
            try {
                this.bgpPeerSingletonService.close();
                this.bgpPeerSingletonService = null;
            } catch (final Exception e) {
                LOG.warn("Failed to close peer instance", e);
            }
        }
    }

    @Override
    public Boolean containsEqualConfiguration(final Neighbor neighbor) {
        final AfiSafis actAfiSafi = this.currentConfiguration.getAfiSafis();
        final AfiSafis extAfiSafi = neighbor.getAfiSafis();
        final List<AfiSafi> actualSafi = actAfiSafi != null ? actAfiSafi.getAfiSafi() : Collections.emptyList();
        final List<AfiSafi> extSafi = extAfiSafi != null ? extAfiSafi.getAfiSafi() : Collections.emptyList();
        return actualSafi.containsAll(extSafi) && extSafi.containsAll(actualSafi)
                && Objects.equals(this.currentConfiguration.getConfig(), neighbor.getConfig())
                && Objects.equals(this.currentConfiguration.getNeighborAddress(), neighbor.getNeighborAddress())
                && Objects.equals(this.currentConfiguration.getAddPaths(), neighbor.getAddPaths())
                && Objects.equals(this.currentConfiguration.getApplyPolicy(), neighbor.getApplyPolicy())
                && Objects.equals(this.currentConfiguration.getAsPathOptions(), neighbor.getAsPathOptions())
                && Objects.equals(this.currentConfiguration.getEbgpMultihop(), neighbor.getEbgpMultihop())
                && Objects.equals(this.currentConfiguration.getGracefulRestart(), neighbor.getGracefulRestart())
                && Objects.equals(this.currentConfiguration.getErrorHandling(), neighbor.getErrorHandling())
                && Objects.equals(this.currentConfiguration.getLoggingOptions(), neighbor.getLoggingOptions())
                && Objects.equals(this.currentConfiguration.getRouteReflector(), neighbor.getRouteReflector())
                && Objects.equals(this.currentConfiguration.getState(), neighbor.getState())
                && Objects.equals(this.currentConfiguration.getTimers(), neighbor.getTimers())
                && Objects.equals(this.currentConfiguration.getTransport(), neighbor.getTransport());
    }

    @Override
    public BGPPeerState getPeerState() {
        if (this.bgpPeerSingletonService == null) {
            return null;
        }
        return this.bgpPeerSingletonService.getPeerState();
    }

    synchronized void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    private final class BgpPeerSingletonService implements BGPPeerStateConsumer, ClusterSingletonService,
            AutoCloseable {
        private final ServiceGroupIdentifier serviceGroupIdentifier;
        private final boolean activeConnection;
        private final BGPDispatcher dispatcher;
        private final InetSocketAddress inetAddress;
        private final int retryTimer;
        private final KeyMapping keys;
        private final BGPPeer bgpPeer;
        private final IpAddress neighborAddress;
        private final BGPSessionPreferences prefs;
        private ClusterSingletonServiceRegistration registration;
        private Future<Void> connection;
        @GuardedBy("this")
        private boolean isServiceInstantiated;

        private BgpPeerSingletonService(final RIB rib, final Neighbor neighbor,
                final BGPTableTypeRegistryConsumer tableTypeRegistry) {
            this.neighborAddress = neighbor.getNeighborAddress();
            final AfiSafis afisSAfis = requireNonNull(neighbor.getAfiSafis());
            final Set<TablesKey> afiSafisAdvertized = OpenConfigMappingUtil
                    .toTableKey(afisSAfis.getAfiSafi(), tableTypeRegistry);
            this.bgpPeer = new BGPPeer(Ipv4Util.toStringIP(this.neighborAddress), rib,
                    OpenConfigMappingUtil.toPeerRole(neighbor), getSimpleRoutingPolicy(neighbor),
                    BgpPeer.this.rpcRegistry, afiSafisAdvertized, Collections.emptySet());
            final List<BgpParameters> bgpParameters = getBgpParameters(neighbor, rib, tableTypeRegistry);
            final KeyMapping keyMapping = OpenConfigMappingUtil.getNeighborKey(neighbor);
            this.prefs = new BGPSessionPreferences(rib.getLocalAs(), getHoldTimer(neighbor), rib.getBgpIdentifier(),
                    getPeerAs(neighbor, rib), bgpParameters, getPassword(keyMapping));
            this.activeConnection = OpenConfigMappingUtil.isActive(neighbor);
            this.dispatcher = rib.getDispatcher();
            this.inetAddress = Ipv4Util.toInetSocketAddress(this.neighborAddress, OpenConfigMappingUtil.getPort(neighbor));
            this.retryTimer = OpenConfigMappingUtil.getRetryTimer(neighbor);
            this.keys = keyMapping;
            this.serviceGroupIdentifier = rib.getRibIServiceGroupIdentifier();
            LOG.info("Peer Singleton Service {} registered", this.serviceGroupIdentifier.getValue());
            //this need to be always the last step
            this.registration = rib.registerClusterSingletonService(this);
        }

        @Override
        public void close() throws Exception {
            if (this.registration != null) {
                this.registration.close();
                this.registration = null;
            }
        }

        @Override
        public synchronized void instantiateServiceInstance() {
            this.isServiceInstantiated = true;
            LOG.info("Peer Singleton Service {} instantiated, Peer {}", getIdentifier().getValue(),
                    this.neighborAddress);
            this.bgpPeer.instantiateServiceInstance();
            this.dispatcher.getBGPPeerRegistry().addPeer(this.neighborAddress, this.bgpPeer, this.prefs);
            if (this.activeConnection) {
                this.connection = this.dispatcher.createReconnectingClient(this.inetAddress, this.retryTimer,
                        this.keys);
            }
        }

        @Override
        public synchronized ListenableFuture<Void> closeServiceInstance() {
            if (!this.isServiceInstantiated) {
                LOG.info("Peer Singleton Service {} already closed, Peer {}", getIdentifier().getValue(),
                        this.neighborAddress);
                return Futures.immediateFuture(null);
            }
            LOG.info("Close Peer Singleton Service {}, Peer {}", getIdentifier().getValue(), this.neighborAddress);
            this.isServiceInstantiated = false;
            if (this.connection != null) {
                this.connection.cancel(true);
                this.connection = null;
            }
            final ListenableFuture<Void> future = this.bgpPeer.close();
            if (BgpPeer.this.currentConfiguration != null) {
                this.dispatcher.getBGPPeerRegistry().removePeer(BgpPeer.this.currentConfiguration.getNeighborAddress());
            }
            return future;
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return this.serviceGroupIdentifier;
        }

        @Override
        public BGPPeerState getPeerState() {
            return this.bgpPeer.getPeerState();
        }
    }
}
