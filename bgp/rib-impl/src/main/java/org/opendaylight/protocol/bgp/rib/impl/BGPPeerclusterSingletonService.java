/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeRegistrator;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPConfigModuleTracker;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPPeerclusterSingletonService implements ClusterSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerclusterSingletonService.class);
    private final RIB rib;
    private final Optional<Rfc2385Key> md5Password;
    private final BGPPeer bgpClientPeer;
    private final BGPPeerModuleTracker moduleTracker;
    private final ServiceGroupIdentifier serviceGroupIdentifier;
    private final Integer retrytimer;
    private final BGPPeerRegistry peerRegistryBackwards;
    private final IpAddress host;
    private final Boolean initiateConnection;
    private final PortNumber port;
    private final BGPSessionPreferences prefs;
    private ClusterSingletonServiceRegistration registration;
    private Future<Void> futureConnection;

    public BGPPeerclusterSingletonService(final RIB rib, final List<BgpParameters> tlvs, final Optional<Rfc2385Key> md5Password,
        final Integer holdtimer, final AsNumber remoteAs, final IpAddress host, final String hostName, final PeerRole peerRole,
        final SimpleRoutingPolicy simpleRoutingPolicy, final RpcProviderRegistry rpcRegistry, final BGPPeerRuntimeRegistrator rootRuntimeBeanRegistratorWrapper,
        final BGPPeerRegistry peerRegistryBackwards, final String instanceName, final Boolean initiateConnection, final PortNumber port,
        final List<BgpTableType> advertizedTableDependency, final List<AddressFamilies> addPathDependency, final Integer retrytimer) {
        this.rib = rib;
        this.md5Password = md5Password;
        this.retrytimer = retrytimer;
        this.bgpClientPeer = new BGPPeer(hostName, this.rib, peerRole, simpleRoutingPolicy, rpcRegistry);
        this.bgpClientPeer.registerRootRuntimeBean(rootRuntimeBeanRegistratorWrapper);
        this.peerRegistryBackwards = peerRegistryBackwards;
        this.host = host;
        this.port = port;

        this.initiateConnection = initiateConnection;
        final BGPPeerInstanceConfiguration bgpPeerInstanceConfiguration = new BGPPeerInstanceConfiguration(new InstanceConfigurationIdentifier(instanceName),
            host, port, holdtimer, peerRole, initiateConnection, advertizedTableDependency, remoteAs, this.md5Password, addPathDependency);
        this.moduleTracker = new BGPPeerModuleTracker(this.rib.getOpenConfigProvider(), bgpPeerInstanceConfiguration);
        this.prefs = new BGPSessionPreferences(this.rib.getLocalAs(), holdtimer, this.rib.getBgpIdentifier(), remoteAs,
            tlvs, getMD5Password(this.md5Password));

        LOG.info("Peer Singleton Service {} registered", this.getIdentifier());
        this.serviceGroupIdentifier = this.rib.getRibIServiceGroupIdentifier();
        this.registration = this.rib.registerClusterSingletonService(this);
    }

    private static final class BGPPeerModuleTracker implements BGPConfigModuleTracker {
        private final BGPOpenconfigMapper<BGPPeerInstanceConfiguration> neighborProvider;
        private final BGPPeerInstanceConfiguration bgpPeerInstanceConfiguration;

        BGPPeerModuleTracker(final Optional<BGPOpenConfigProvider> openConfigProvider, final BGPPeerInstanceConfiguration
            bgpPeerInstanceConfiguration) {
            if (openConfigProvider.isPresent()) {
                this.neighborProvider = openConfigProvider.get().getOpenConfigMapper(BGPPeerInstanceConfiguration.class);
            } else {
                this.neighborProvider = null;
            }
            this.bgpPeerInstanceConfiguration = bgpPeerInstanceConfiguration;
        }

        @Override
        public void onInstanceCreate() {
            if (this.neighborProvider != null) {
                this.neighborProvider.writeConfiguration(this.bgpPeerInstanceConfiguration);
            }
        }

        @Override
        public void onInstanceClose() {
            if (this.neighborProvider != null) {
                this.neighborProvider.removeConfiguration(this.bgpPeerInstanceConfiguration);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

    private static Optional<byte[]> getMD5Password(final Optional<Rfc2385Key> password) {
        return password.isPresent() ? Optional.of(password.get().getValue().getBytes(StandardCharsets.US_ASCII)) : Optional.absent();
    }

    private io.netty.util.concurrent.Future<Void> initiateConnection(final InetSocketAddress address, final Optional<Rfc2385Key> password, final BGPPeerRegistry registry) {
        final KeyMapping keys = KeyMapping.getKeyMapping(address.getAddress(), password);
        final Optional<KeyMapping> optionalKey = Optional.fromNullable(keys);
        return this.rib.getDispatcher().createReconnectingClient(address, registry, this.retrytimer, optionalKey);
    }

    @Override
    public void instantiateServiceInstance() {
        this.bgpClientPeer.instantiateServiceInstance();
        this.moduleTracker.onInstanceCreate();
        this.peerRegistryBackwards.addPeer(this.host, bgpClientPeer, prefs);
        // Initiate connection
        if (this.initiateConnection) {
            this.futureConnection = initiateConnection(createAddress(), this.md5Password, this.peerRegistryBackwards);
        }
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        if (this.futureConnection != null) {
            futureConnection.cancel(true);
        }
        bgpClientPeer.close();
        this.peerRegistryBackwards.removePeer(this.host);
        moduleTracker.onInstanceClose();
        return Futures.immediateFuture(null);
    }

    private InetSocketAddress createAddress() {
        Preconditions.checkArgument(this.host.getIpv4Address() != null || this.host.getIpv6Address() != null, "Failed to handle host %s", this.host);
        final Integer portNumber = this.port.getValue();
        if (this.host.getIpv4Address() != null) {
            return new InetSocketAddress(InetAddresses.forString(this.host.getIpv4Address().getValue()), portNumber);
        }
        return new InetSocketAddress(InetAddresses.forString(this.host.getIpv6Address().getValue()), portNumber);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return this.serviceGroupIdentifier;
    }
}
