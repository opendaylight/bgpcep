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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerExportGroupRegistry;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class PeerExportGroupImpl implements PeerExportGroupRegistry {
    private final Map<PeerId, PeerExporTuple> peers = Collections.synchronizedMap(new HashMap<PeerId, PeerExporTuple>());

    private final AbstractExportPolicy policy;

    public PeerExportGroupImpl(final AbstractExportPolicy policy) {
        this.policy = Preconditions.checkNotNull(policy);
    }

    @Override
    public ContainerNode effectiveAttributes(final PeerRole role, final ContainerNode attributes) {
        return attributes == null || role == null ? null : this.policy.effectiveAttributes(role, attributes);
    }

    @Override
    public Collection<Map.Entry<PeerId, PeerExporTuple>> getPeers() {
        return this.peers.entrySet();
    }

    @Override
    public boolean containsPeer(final PeerId routePeerId) {
        return this.peers.containsKey(routePeerId);
    }

    @Override
    public AbstractRegistration registerPeer(final PeerId peerId, final PeerExporTuple peerExporTuple) {
        this.peers.put(peerId, peerExporTuple);
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                PeerExportGroupImpl.this.peers.remove(peerId);
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return this.peers.isEmpty();
    }
}