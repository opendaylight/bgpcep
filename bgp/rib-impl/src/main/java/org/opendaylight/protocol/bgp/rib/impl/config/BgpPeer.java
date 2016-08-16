/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getHoldTimer;
import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getPeerAs;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer.WriteConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
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
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BgpPeer implements PeerBean, BGPPeerRuntimeMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(BgpPeer.class);

    private final RpcProviderRegistry rpcRegistry;
    private final BGPPeerRegistry peerRegistry;
    private ServiceRegistration<?> serviceRegistration;
    private Neighbor currentConfiguration;
    private BgpPeerSingletonService bgpPeerSingletonService;

    public BgpPeer(final RpcProviderRegistry rpcRegistry, final BGPPeerRegistry peerRegistry) {
        this.rpcRegistry = rpcRegistry;
        this.peerRegistry = peerRegistry;
    }

    @Override
    public void start(final RIB rib, final Neighbor neighbor, final BGPOpenConfigMappingService mappingService,
        final WriteConfiguration configurationWriter) {
        Preconditions.checkState(this.bgpPeerSingletonService == null, "Previous peer instance {} was not closed.");
        this.bgpPeerSingletonService = new BgpPeerSingletonService(rib, neighbor, mappingService, configurationWriter);
        this.currentConfiguration = neighbor;
    }

    @Override
    public void restart(final RIB rib, final BGPOpenConfigMappingService mappingService) {
        Preconditions.checkState(this.currentConfiguration != null);
        start(rib, this.currentConfiguration, mappingService, null);
    }

    @Override
    public void close() {
        try {
            this.bgpPeerSingletonService.close();
            this.bgpPeerSingletonService = null;
        } catch (final Exception e) {
            LOG.warn("Failed to close peer instance", e);
        }
        this.currentConfiguration = null;
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
    }

    @Override
    public Boolean containsEqualConfiguration(final Neighbor neighbor) {
        return this.currentConfiguration.equals(neighbor);
    }

    private static List<BgpParameters> getBgpParameters(final Neighbor neighbor, final RIB rib,
            final BGPOpenConfigMappingService mappingService) {
        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> caps = new ArrayList<>();
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().setAs4BytesCapability(
                new As4BytesCapabilityBuilder().setAsNumber(rib.getLocalAs()).build()).build()).build());

        caps.add(new OptionalCapabilitiesBuilder().setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(MultiprotocolCapabilitiesUtil.RR_CAPABILITY).build());

        final List<AddressFamilies> addPathCapability = mappingService.toAddPathCapability(neighbor.getAfiSafis().getAfiSafi());
        if (!addPathCapability.isEmpty()) {
            caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
                    new CParameters1Builder().setAddPathCapability(
                            new AddPathCapabilityBuilder().setAddressFamilies(addPathCapability).build()).build()).build()).build());
        }

        final List<BgpTableType> tableTypes = mappingService.toTableTypes(neighbor.getAfiSafis().getAfiSafi());
        for (final BgpTableType tableType : tableTypes) {
            if (!rib.getLocalTables().contains(tableType)) {
                LOG.info("RIB instance does not list {} in its local tables. Incoming data will be dropped.", tableType);
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
    public BgpPeerState getBgpPeerState() {
        return this.bgpPeerSingletonService.getPeer().getBgpPeerState();
    }

    @Override
    public BgpSessionState getBgpSessionState() {
        return this.bgpPeerSingletonService.getPeer().getBgpSessionState();
    }

    @Override
    public void resetSession() {
        this.bgpPeerSingletonService.getPeer().resetSession();
    }

    @Override
    public void resetStats() {
        this.bgpPeerSingletonService.getPeer().resetStats();
    }

    void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    private final class BgpPeerSingletonService implements ClusterSingletonService, AutoCloseable {
        private final ServiceGroupIdentifier serviceGroupIdentifier;
        private final boolean activeConnection;
        private final BGPDispatcher dispatcher;
        private final InetSocketAddress inetAddress;
        private final int retryTimer;
        private final Optional<KeyMapping> key;
        private final WriteConfiguration configurationWriter;
        private ClusterSingletonServiceRegistration registration;
        private final BGPPeer bgpPeer;
        private final IpAddress neighborAddress;
        private final BGPSessionPreferences prefs;
        private Future<Void> connection;

        private BgpPeerSingletonService(final RIB rib, final Neighbor neighbor, final BGPOpenConfigMappingService mappingService,
            final WriteConfiguration configurationWriter) {
            this.neighborAddress = neighbor.getNeighborAddress();
            this.bgpPeer = new BGPPeer(Ipv4Util.toStringIP(this.neighborAddress), rib, mappingService.toPeerRole(neighbor), rpcRegistry);
            final List<BgpParameters> bgpParameters = getBgpParameters(neighbor, rib, mappingService);
            final KeyMapping key = OpenConfigMappingUtil.getNeighborKey(neighbor);
            this.prefs = new BGPSessionPreferences(rib.getLocalAs(), getHoldTimer(neighbor), rib.getBgpIdentifier(), getPeerAs(neighbor, rib),
                bgpParameters, getPassword(key));
            this.activeConnection = OpenConfigMappingUtil.isActive(neighbor);
            this.dispatcher = rib.getDispatcher();
            this.inetAddress = Ipv4Util.toInetSocketAddress(this.neighborAddress, OpenConfigMappingUtil.getPort(neighbor));
            this.retryTimer = OpenConfigMappingUtil.getRetryTimer(neighbor);
            this.key = Optional.fromNullable(key);
            this.serviceGroupIdentifier = rib.getRibIServiceGroupIdentifier();
            LOG.info("Peer Singleton Service {} registered", this.serviceGroupIdentifier);
            this.registration = rib.registerClusterSingletonService(this);
            this.configurationWriter = configurationWriter;
        }

        @Override
        public void close() throws Exception {
            if (this.registration != null) {
                this.registration.close();
                this.registration = null;
            }
        }

        @Override
        public void instantiateServiceInstance() {
            if(this.configurationWriter != null) {
                this.configurationWriter.apply();
            }
            LOG.info("Peer Singleton Service {} instantiated", getIdentifier());
            this.bgpPeer.instantiateServiceInstance();
            peerRegistry.addPeer(this.neighborAddress, this.bgpPeer, prefs);
            if (this.activeConnection) {
                this.connection = this.dispatcher.createReconnectingClient(this.inetAddress, peerRegistry, this.retryTimer, this.key);
            }
        }

        @Override
        public ListenableFuture<Void> closeServiceInstance() {
            LOG.info("Close RIB Singleton Service {}", this.getIdentifier());
            if (this.connection != null) {
                this.connection.cancel(true);
                this.connection = null;
            }
            this.bgpPeer.close();
            if(currentConfiguration != null) {
                peerRegistry.removePeer(currentConfiguration.getNeighborAddress());
            }
            return Futures.immediateFuture(null);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return this.serviceGroupIdentifier;
        }

        BGPPeerRuntimeMXBean getPeer() {
            return this.bgpPeer;
        }
    }
}
