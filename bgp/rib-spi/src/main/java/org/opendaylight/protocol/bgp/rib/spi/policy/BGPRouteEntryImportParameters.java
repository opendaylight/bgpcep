/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;

public interface BGPRouteEntryImportParameters extends RouteEntryKey {
    /**
     * Peer id of Peer route entry announcer.
     *
     * @return peer Id of announcer Peer
     */
    @Nonnull
    PeerId getFromPeerId();

    /**
     * Peer role of Peer route entry announcer.
     *
     * @return role of the peer which originated the routes
     */
    @Nonnull
    PeerRole getFromPeerRole();
}
