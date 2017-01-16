/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Collection;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * A collection of peers sharing the same export policy.
 *
 * @deprecated Replaced by {@link BGPPeerTracker}
 */
public interface PeerExportGroup {
    /**
     * Transform outgoing attributes according to policy per Peer
     *
     * @param role       root Peer role
     * @param attributes attributes container
     * @return return attributes container after apply policy
     */
    ContainerNode effectiveAttributes(PeerRole role, ContainerNode attributes);

    /**
     * @return map of peer
     */
    Collection<Map.Entry<PeerId, PeerExporTuple>> getPeers();

    boolean containsPeer(PeerId routePeerId);

    final class PeerExporTuple {
        private final YangInstanceIdentifier yii;
        private final PeerRole role;

        public PeerExporTuple(final YangInstanceIdentifier yii, final PeerRole role) {
            this.yii = yii;
            this.role = role;
        }

        public YangInstanceIdentifier getYii() {
            return this.yii;
        }

        public PeerRole getRole() {
            return this.role;
        }
    }
}
