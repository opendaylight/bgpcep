/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: bgp-peer
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Sat Jan 25 11:00:14 CET 2014
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPConfigModuleTracker;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class BGPPeerModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPPeerModule {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerModule.class);

    public BGPPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BGPPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final BGPPeerModule oldModule,
        final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        JmxAttributeValidationException.checkNotNull(getHost(), "value is not set.", hostJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getPort(), "value is not set.", portJmxAttribute);

        if (getOptionaPassword(getPassword()).isPresent()) {
            /*
             *  This is a nasty hack, but we don't have another clean solution. We cannot allow
             *  password being set if the injected dispatcher does not have the optional
             *  md5-server-channel-factory set.
             *
             *  FIXME: this is a use case for Module interfaces, e.g. RibImplModule
             *         should something like isMd5ServerSupported()
             */

            final RIBImplModuleMXBean ribProxy = this.dependencyResolver.newMXBeanProxy(getRib(), RIBImplModuleMXBean.class);
            final BGPDispatcherImplModuleMXBean bgpDispatcherProxy = this.dependencyResolver.newMXBeanProxy(
                ribProxy.getBgpDispatcher(), BGPDispatcherImplModuleMXBean.class);
            final boolean isMd5Supported = bgpDispatcherProxy.getMd5ChannelFactory() != null;

            JmxAttributeValidationException.checkCondition(isMd5Supported,
                "Underlying dispatcher does not support MD5 clients", passwordJmxAttribute);

        }

        if (getPeerRole() != null) {
            final boolean isNotPeerRoleInternal= getPeerRole() != PeerRole.Internal;
            JmxAttributeValidationException.checkCondition(isNotPeerRoleInternal,
                "Internal Peer Role is reserved for Application Peer use.", peerRoleJmxAttribute);
        }
    }

    private InetSocketAddress createAddress() {
        final IpAddress ip = getHost();
        Preconditions.checkArgument(ip.getIpv4Address() != null || ip.getIpv6Address() != null, "Failed to handle host %s", ip);
        if (ip.getIpv4Address() != null) {
            return new InetSocketAddress(InetAddresses.forString(ip.getIpv4Address().getValue()), getPort().getValue());
        }
        return new InetSocketAddress(InetAddresses.forString(ip.getIpv6Address().getValue()), getPort().getValue());
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final RIB r = getRibDependency();

        final List<BgpParameters> tlvs = getTlvs(r);
        final AsNumber remoteAs = getAsOrDefault(r);
        final BGPSessionPreferences prefs = new BGPSessionPreferences(r.getLocalAs(), getHoldtimer(), r.getBgpIdentifier(), remoteAs, tlvs);
        final BGPPeer bgpClientPeer;

        if (getPeerRole() != null) {
            bgpClientPeer = new BGPPeer(peerName(getHostWithoutValue()), r, getPeerRole());
        } else {
            bgpClientPeer = new BGPPeer(peerName(getHostWithoutValue()), r, PeerRole.Ibgp);
        }

        bgpClientPeer.registerRootRuntimeBean(getRootRuntimeBeanRegistratorWrapper());

        getPeerRegistryBackwards().addPeer(getHostWithoutValue(), bgpClientPeer, prefs);

        final BGPPeerModuleTracker moduleTracker = new BGPPeerModuleTracker(r.getOpenConfigProvider());
        moduleTracker.onInstanceCreate();

        final CloseableNoEx peerCloseable = new CloseableNoEx() {
            @Override
            public void close() {
                bgpClientPeer.close();
                getPeerRegistryBackwards().removePeer(getHostWithoutValue());
                moduleTracker.onInstanceClose();
            }
        };

        // Initiate connection
        if(getInitiateConnection()) {
            final Future<Void> cf = initiateConnection(createAddress(), getOptionaPassword(getPassword()), getPeerRegistryBackwards());
            return new CloseableNoEx() {
                @Override
                public void close() {
                    cf.cancel(true);
                    peerCloseable.close();
                }
            };
        } else {
            return peerCloseable;
        }
    }

    private interface CloseableNoEx extends AutoCloseable {
        @Override
        void close();
    }

    private AsNumber getAsOrDefault(final RIB r) {
        // Remote AS number defaults to our local AS
        final AsNumber remoteAs;
        if (getRemoteAs() != null) {
            remoteAs = new AsNumber(getRemoteAs());
        } else {
            remoteAs = r.getLocalAs();
        }
        return remoteAs;
    }

    private List<BgpParameters> getTlvs(final RIB r) {
        final List<BgpParameters> tlvs = new ArrayList<>();
        final List<OptionalCapabilities> caps = new ArrayList<>();
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().setAs4BytesCapability(
            new As4BytesCapabilityBuilder().setAsNumber(r.getLocalAs()).build()).build()).build());
        caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build()).build()).build()).build());

        for (final BgpTableType t : getAdvertizedTableDependency()) {
            if (!r.getLocalTables().contains(t)) {
                LOG.info("RIB instance does not list {} in its local tables. Incoming data will be dropped.", t);
            }

            caps.add(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder(t).build()).build()).build()).build());
        }
        tlvs.add(new BgpParametersBuilder().setOptionalCapabilities(caps).build());
        return tlvs;
    }

    public IpAddress getHostWithoutValue() {
        // FIXME we need to remove field "value" from IpAddress since equals does not work as expected when value being present
        // Remove after this bug is fixed https://bugs.opendaylight.org/show_bug.cgi?id=1276
        final IpAddress host = super.getHost();
        Preconditions.checkArgument(host.getIpv4Address() != null || host.getIpv6Address() != null, "Unexpected host %s", host);
        if(host.getIpv4Address() != null) {
            return new IpAddress(host.getIpv4Address());
        }
        return new IpAddress(host.getIpv6Address());
    }

    private io.netty.util.concurrent.Future<Void> initiateConnection(final InetSocketAddress address, final Optional<Rfc2385Key> password, final BGPPeerRegistry registry) {
        KeyMapping keys = null;
        if (password.isPresent()) {
            keys = new KeyMapping();
            keys.put(address.getAddress(), password.get().getValue().getBytes(Charsets.US_ASCII));
        }

        final RIB rib = getRibDependency();
        final Optional<KeyMapping> optionalKey = Optional.fromNullable(keys);
        return rib.getDispatcher().createReconnectingClient(address, registry, rib.getTcpStrategyFactory(), optionalKey);
    }

    private BGPPeerRegistry getPeerRegistryBackwards() {
        return getPeerRegistry() == null ? StrictBGPPeerRegistry.GLOBAL : getPeerRegistryDependency();
    }

    private static String peerName(final IpAddress host) {
        if (host.getIpv4Address() != null) {
            return host.getIpv4Address().getValue();
        }
        if (host.getIpv6Address() != null) {
            return host.getIpv6Address().getValue();
        }

        return null;
    }

    private final class BGPPeerModuleTracker implements BGPConfigModuleTracker {

        private final BGPOpenconfigMapper<BGPPeerInstanceConfiguration> neighborProvider;
        private final InstanceConfigurationIdentifier identifier;

        public BGPPeerModuleTracker(final Optional<BGPOpenConfigProvider> openconfigProvider) {
            if (openconfigProvider.isPresent()) {
                neighborProvider = openconfigProvider.get().getOpenConfigMapper(BGPPeerInstanceConfiguration.class);
            } else {
                neighborProvider = null;
            }
            identifier = new InstanceConfigurationIdentifier(getIdentifier().getInstanceName());
        }

        @Override
        public void onInstanceCreate() {
            if (neighborProvider != null) {
                neighborProvider.writeConfiguration(new BGPPeerInstanceConfiguration(identifier,
                        getHostWithoutValue(), getPort(), getHoldtimer(), getPeerRole(), getInitiateConnection(),
                        getAdvertizedTableDependency(), getAsOrDefault(getRibDependency()), getOptionaPassword(getPassword())));
            }
        }

        @Override
        public void onInstanceClose() {
            if (neighborProvider != null) {
                neighborProvider.removeConfiguration(identifier);
            }
        }

    }

    private Optional<Rfc2385Key> getOptionaPassword(Rfc2385Key password) {
        return password != null && ! password.getValue().isEmpty() ? Optional.fromNullable(password) : Optional.<Rfc2385Key>absent();
    }

}
