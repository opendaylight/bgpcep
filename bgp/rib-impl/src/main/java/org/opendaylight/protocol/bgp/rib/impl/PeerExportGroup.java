/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ExportPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * A collection of peers sharing the same export policy.
 */
final class PeerExportGroup {
    private final Collection<Entry<PeerId, YangInstanceIdentifier>> peers;
    private final Map<PeerId, PeerRole> peerRoles;
    private final ExportPolicy policy;

    PeerExportGroup(final Collection<Entry<PeerId, YangInstanceIdentifier>> peers, final Map<PeerId, PeerRole> peerRoles, final ExportPolicy policy) {
        this.peers = Preconditions.checkNotNull(peers);
        this.peerRoles = Preconditions.checkNotNull(peerRoles);
        this.policy = Preconditions.checkNotNull(policy);
    }

    ContainerNode effectiveAttributes(final PeerId sourcePeerId, final ContainerNode attributes) {
        return attributes == null ? null :  policy.effectiveAttributes(peerRoles.get(sourcePeerId), attributes);
    }

    Collection<Entry<PeerId, YangInstanceIdentifier>> getPeers() {
        return peers;
    }
}