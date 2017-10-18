/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerExportGroupRegistry;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class PeerExportGroupImpl implements PeerExportGroupRegistry {
    @GuardedBy("this")
    private final Map<PeerId, PeerExporTuple> peers = new HashMap<>();
    private final AbstractExportPolicy policy;

    public PeerExportGroupImpl(final AbstractExportPolicy policy) {
        this.policy = requireNonNull(policy);
    }

    @Override
    public ContainerNode effectiveAttributes(final PeerRole role, final ContainerNode attributes) {
        return attributes == null || role == null ? null : this.policy.effectiveAttributes(role, attributes);
    }

    @Override
    public boolean containsPeer(final PeerId routePeerId) {
        synchronized (this.peers) {
            return this.peers.containsKey(routePeerId);
        }
    }

    @Override
    public void forEach(final BiConsumer<PeerId, YangInstanceIdentifier> action) {
        synchronized (this.peers) {
            for (final Map.Entry<PeerId, PeerExporTuple> pid : this.peers.entrySet()) {
                action.accept(pid.getKey(), pid.getValue().getYii());
            }
        }
    }

    @Override
    public AbstractRegistration registerPeer(final PeerId peerId, final PeerExporTuple peerExporTuple) {
        synchronized (this.peers) {
            this.peers.put(peerId, peerExporTuple);
        }

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (PeerExportGroupImpl.this.peers) {
                    PeerExportGroupImpl.this.peers.remove(peerId);
                }
            }
        };
    }

    @Override
    public boolean isEmpty() {
        synchronized (this.peers) {
            return this.peers.isEmpty();
        }
    }
}