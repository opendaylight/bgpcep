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
import io.netty.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
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


public class BgpPeer implements AutoCloseable, BGPPeerRuntimeMXBean {

    //FIXME make configurable
    private static final PortNumber PORT = new PortNumber(179);

    private static final Logger LOG = LoggerFactory.getLogger(BgpPeer.class);

    private final RpcProviderRegistry rpcRegistry;
    private final BGPPeerRegistry peerRegistry;
    private BGPPeer bgpPeer;

    private IpAddress neighborAddress;

    private Future<Void> connection;

    private ServiceRegistration<?> serviceRegistration;

    public BgpPeer(final RpcProviderRegistry rpcRegistry, final BGPPeerRegistry peerRegistry) {
        this.rpcRegistry = rpcRegistry;
        this.peerRegistry = peerRegistry;
    }

    public void start(final RIB rib, final Neighbor neighbor, final BGPOpenConfigMappingService mappingService) {
        Preconditions.checkState(this.bgpPeer == null, "Previous peer instance {} was not closed.");
        this.neighborAddress = neighbor.getNeighborAddress();
        this.bgpPeer = new BGPPeer(Ipv4Util.toStringIP(this.neighborAddress), rib,
                mappingService.toPeerRole(neighbor), this.rpcRegistry);
        final List<BgpParameters> bgpParameters = getBgpParameters(neighbor, rib, mappingService);
        final KeyMapping key = OpenConfigMappingUtil.getNeighborKey(neighbor);
        final BGPSessionPreferences prefs = new BGPSessionPreferences(rib.getLocalAs(),
                getHoldTimer(neighbor), rib.getBgpIdentifier(), getPeerAs(neighbor, rib), bgpParameters, getPassword(key));
        this.peerRegistry.addPeer(this.neighborAddress, this.bgpPeer, prefs);
        if (OpenConfigMappingUtil.isActive(neighbor)) {
            this.connection = rib.getDispatcher().createReconnectingClient(
                    Ipv4Util.toInetSocketAddress(this.neighborAddress, PORT), this.peerRegistry,
                    OpenConfigMappingUtil.getRetryTimer(neighbor), Optional.fromNullable(key));
        }

    }

    @Override
    public void close() {
        if (this.bgpPeer != null) {
            if (this.connection != null) {
                this.connection.cancel(true);
                this.connection = null;
            }
            this.bgpPeer.close();
            this.bgpPeer = null;
            this.peerRegistry.removePeer(this.neighborAddress);
            this.neighborAddress = null;
            if (this.serviceRegistration != null) {
                this.serviceRegistration.unregister();
                this.serviceRegistration = null;
            }
        }
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
        return this.bgpPeer.getBgpPeerState();
    }

    @Override
    public BgpSessionState getBgpSessionState() {
        return this.bgpPeer.getBgpSessionState();
    }

    @Override
    public void resetStats() {
        this.bgpPeer.resetStats();

    }

    @Override
    public void resetSession() {
        this.bgpPeer.resetSession();

    }

    public void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

}
