/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactoryDependencies;

final class PCEPSessionNegotiatorFactoryDependenciesImpl implements PCEPSessionNegotiatorFactoryDependencies {
    private final @NonNull PCEPSessionListenerFactory listenerFactory;
    private final @NonNull PCEPPeerProposal peerProposal;

    PCEPSessionNegotiatorFactoryDependenciesImpl(final PCEPSessionListenerFactory listenerFactory,
            final PCEPPeerProposal peerProposal) {
        this.listenerFactory = requireNonNull(listenerFactory);
        this.peerProposal = requireNonNull(peerProposal);
    }

    @Override
    public PCEPSessionListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    @Override
    public PCEPPeerProposal getPeerProposal() {
        return peerProposal;
    }
}
