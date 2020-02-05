/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;

/**
 * Contains Peer destiny information for import route entry.
 */
@NonNullByDefault
public interface BGPRouteEntryImportParameters {
    /**
     * Peer id of Peer route entry announcer.
     *
     * @return peer Role of announcer Peer
     */
    PeerRole getFromPeerRole();

    /**
     * Peer id of Peer route entry announcer.
     *
     * @return peer Id of announcer Peer
     */
    PeerId getFromPeerId();

    /**
     * Peer id of Peer route entry announcer.
     *
     * @return peer Id of announcer Peer
     */
    @Nullable ClusterIdentifier getFromClusterId();

    /**
     * Peer local AS of route entry announcer.
     *
     * @return peer Local AS
     */
    @Nullable AsNumber getFromPeerLocalAs();
}
