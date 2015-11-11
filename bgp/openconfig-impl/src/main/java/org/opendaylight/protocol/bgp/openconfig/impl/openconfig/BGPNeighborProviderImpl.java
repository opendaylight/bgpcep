/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import com.google.common.base.Optional;
import java.math.BigDecimal;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.RouteReflectorBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;

final class BGPNeighborProviderImpl extends AbstractBGPNeighborProvider<BGPPeerInstanceConfiguration> {

    public BGPNeighborProviderImpl(final BindingTransactionChain txChain, final BGPConfigStateStore stateHolders) {
        super(txChain, stateHolders, Neighbor.class);
    }

    @Override
    public Class<BGPPeerInstanceConfiguration> getInstanceConfigurationType() {
        return BGPPeerInstanceConfiguration.class;
    }

    @Override
    public Neighbor apply(final BGPPeerInstanceConfiguration config) {
        return toNeighborConfiguration(config);
    }

    @Override
    public ModuleKey createModuleKey(final String instanceName) {
        return new ModuleKey(instanceName, BgpPeer.class);
    }

    private static Neighbor toNeighborConfiguration(final BGPPeerInstanceConfiguration config) {
        return new NeighborBuilder()
            .setNeighborAddress(config.getHost())
            .setKey(new NeighborKey(config.getHost()))
            .setTransport(new TransportBuilder().setConfig(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.ConfigBuilder()
                    .setPassiveMode(!config.isActive())
                    .build()).build())
            .setConfig(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder()
                    .setAuthPassword(getPasswor(config.getPassword()))
                    .setPeerAs(config.getAsNumber())
                    .setPeerType(toPeerTye(config.getPeerRole()))
                    .build())
            .setAfiSafis(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder()
                    .setAfiSafi(OpenConfigUtil.toAfiSafis(config.getAdvertizedTables()))
                    .build())
            .setTimers(new TimersBuilder().setConfig(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.ConfigBuilder()
                    .setHoldTime(BigDecimal.valueOf(config.getHoldTimer()))
                    .build()).build())
            .setRouteReflector(new RouteReflectorBuilder().setConfig(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.route.reflector.ConfigBuilder()
                    .setRouteReflectorClient(config.getPeerRole() == PeerRole.RrClient)
                    .build()).build())
            .build();
    }

    private static PeerType toPeerTye(final PeerRole peerRole) {
        switch(peerRole) {
        case Ibgp:
        case RrClient:
            return PeerType.INTERNAL;
        case Ebgp:
            return PeerType.EXTERNAL;
        case Internal:
            break;
        default:
            break;
        }
        return null;
    }

    private static String getPasswor(final Optional<Rfc2385Key> maybePassword) {
        if (maybePassword.isPresent()) {
            return maybePassword.get().getValue();
        }
        return null;
    }
}
