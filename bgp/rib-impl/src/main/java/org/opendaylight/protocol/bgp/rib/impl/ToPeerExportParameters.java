/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteTarget;

@NonNullByDefault
record ToPeerExportParameters(
        Peer fromPeer,
        AbstractPeer toPeer,
        String routeKey) implements BGPRouteEntryExportParameters {
    ToPeerExportParameters {
        requireNonNull(fromPeer);
        requireNonNull(toPeer);
        requireNonNull(routeKey);
    }

    @Override
    public PeerRole getFromPeerRole() {
        return fromPeer.getRole();
    }

    @Override
    public PeerId getFromPeerId() {
        return fromPeer.getPeerId();
    }

    @Override
    public @Nullable ClusterIdentifier getFromClusterId() {
        return fromPeer.getClusterId();
    }

    @Override
    public @Nullable AsNumber getFromPeerLocalAs() {
        return fromPeer.getLocalAs();
    }

    @Override
    public PeerRole getToPeerRole() {
        return toPeer.getRole();
    }

    @Override
    public @Nullable AsNumber getToPeerLocalAs() {
        return toPeer.getLocalAs();
    }

    @Override
    public String getRouteKey() {
        return routeKey;
    }

    @Override
    public PeerId getToPeerId() {
        return toPeer.getPeerId();
    }

    @Override
    public List<RouteTarget> getMemberships() {
        return toPeer.getMemberships();
    }

    @Override
    public List<Route> getClientRouteTargetContrainCache() {
        return toPeer.rtCache.getClientRouteTargetContrainCache();
    }
}
