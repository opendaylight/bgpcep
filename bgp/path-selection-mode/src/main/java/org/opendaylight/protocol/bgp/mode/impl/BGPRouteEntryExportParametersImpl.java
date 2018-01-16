/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class BGPRouteEntryExportParametersImpl implements BGPRouteEntryExportParameters {
    private final NodeIdentifierWithPredicates routeId;
    private final PeerId fromPeerId;
    private final PeerRole fromPeerRole;
    private final PeerId toPeerId;
    private final PeerRole toPeerRole;

    public BGPRouteEntryExportParametersImpl(
            final NodeIdentifierWithPredicates routeId,
            final PeerId fromPeerId,
            final PeerRole fromPeerRole,
            final PeerId toPeerId,
            final PeerRole toPeerRole) {
        this.routeId = routeId;
        this.fromPeerId = fromPeerId;
        this.fromPeerRole = fromPeerRole;
        this.toPeerId = toPeerId;
        this.toPeerRole = toPeerRole;
    }

    @Nonnull
    @Override
    public PeerId getToPeer() {
        return this.toPeerId;
    }

    @Nonnull
    @Override
    public PeerRole getToPeerRole() {
        return this.toPeerRole;
    }

    @Nonnull
    @Override
    public PeerId getFromPeerId() {
        return this.fromPeerId;
    }

    @Nonnull
    @Override
    public PeerRole getFromPeerRole() {
        return this.fromPeerRole;
    }

    @Nonnull
    @Override
    public NodeIdentifierWithPredicates getRouteId() {
        return this.routeId;
    }
}
