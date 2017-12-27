/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public final class PeerRoleUtil {
    public static final NodeIdentifier PEER_ROLE_NID = new NodeIdentifier(QName.create(Peer.QNAME,
            "peer-role").intern());

    private PeerRoleUtil() {
        throw new UnsupportedOperationException();
    }

    public static PeerRole roleForChange(final Optional<NormalizedNode<?, ?>> maybePeerRole) {
        if (maybePeerRole.isPresent()) {
            final LeafNode<?> peerRoleLeaf = (LeafNode<?>) maybePeerRole.get();
            return PeerRole.valueOf(BindingMapping.getClassName((String) peerRoleLeaf.getValue()));
        }
        return null;
    }

    public static String roleForString(final PeerRole role) {
        switch (role) {
            case Ebgp:
                return "ebgp";
            case Ibgp:
                return "ibgp";
            case RrClient:
                return "rr-client";
            case Internal:
                return "internal";
            default:
                throw new IllegalArgumentException("Unhandled role " + role);
        }
    }
}
