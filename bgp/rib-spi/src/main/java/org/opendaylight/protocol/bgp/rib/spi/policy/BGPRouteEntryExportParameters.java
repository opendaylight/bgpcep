/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;

/**
 * Contains Peer destiny information for export route entry.
 */
public interface BGPRouteEntryExportParameters extends BGPRouteEntryImportParameters {
    /**
     * Peer id of Peer destiny for route entry.
     *
     * @return peer Id of announced Peer
     */
    @Nonnull
    PeerId getToPeerId();

    /**
     * Peer role of Peer destiny for route entry.
     *
     * @return peer role of announced Peer
     */
    @Nonnull
    PeerRole getToPeerRole();
}
