/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import java.util.function.BiConsumer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * A collection of peers sharing the same export policy.
 */
public interface PeerExportGroup {
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

    /**
     * Transform outgoing attributes according to policy per Peer
     *
     * @param role root Peer role
     * @param attributes attributes container
     * @return return attributes container after apply policy
     */
    ContainerNode effectiveAttributes(PeerRole role, ContainerNode attributes);

    /**
     *
     * @param routePeerId PeerId
     * @return true if peer is present on this export group
     */
    boolean containsPeer(PeerId routePeerId);

    /**
     * Applies the given action for each entry in this PeerExportGroup on synchronized mode
     *
     * @param action action to be applied
     */
    void forEach(BiConsumer<PeerId, YangInstanceIdentifier> action);
}
