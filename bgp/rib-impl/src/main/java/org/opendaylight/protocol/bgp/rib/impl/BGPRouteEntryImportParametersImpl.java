/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class BGPRouteEntryImportParametersImpl implements BGPRouteEntryImportParameters {
    private final PeerId peerId;
    private final NodeIdentifierWithPredicates routeId;
    private final PeerRole fromPeerRole;

    public BGPRouteEntryImportParametersImpl(
            final NodeIdentifierWithPredicates routeId,
            final PeerId peerId,
            final PeerRole fromPeerRole) {
        this.routeId = routeId;
        this.peerId = peerId;
        this.fromPeerRole = fromPeerRole;
    }

    @Override
    public NodeIdentifierWithPredicates getRouteId() {
        return this.routeId;
    }

    @Override
    public PeerId getFromPeerId() {
        return this.peerId;
    }

    @Override
    public PeerRole getFromPeerRole() {
        return this.fromPeerRole;
    }

}
