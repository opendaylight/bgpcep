/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

final class PCEPDispatcherDependenciesImpl implements PCEPDispatcherDependencies {
    private final @NonNull PCEPSessionListenerFactory listenerFactory;
    private final @NonNull PCEPPeerProposal peerProposal;

    private final @NonNull PCEPTopologyConfiguration topologyConfig;

    PCEPDispatcherDependenciesImpl(final PCEPSessionListenerFactory listenerFactory,
            final PCEPPeerProposal peerProposal, final PCEPTopologyConfiguration topologyConfig) {
        this.listenerFactory = requireNonNull(listenerFactory);
        this.peerProposal = requireNonNull(peerProposal);
        this.topologyConfig = requireNonNull(topologyConfig);
    }

    @Override
    public PCEPSessionListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    @Override
    public PCEPPeerProposal getPeerProposal() {
        return peerProposal;
    }

    @Override
    public InetSocketAddress getAddress() {
        return topologyConfig.getAddress();
    }

    @Override
    public KeyMapping getKeys() {
        return topologyConfig.getKeys();
    }
}
