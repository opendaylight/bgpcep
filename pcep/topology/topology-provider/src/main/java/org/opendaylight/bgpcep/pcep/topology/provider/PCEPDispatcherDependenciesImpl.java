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
import javax.annotation.Nonnull;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyConfiguration;
import org.opendaylight.protocol.pcep.SpeakerIdMapping;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

public class PCEPDispatcherDependenciesImpl implements PCEPDispatcherDependencies {
    private final ServerSessionManager manager;
    private final PCEPTopologyConfiguration topologyConfigDependencies;

    public PCEPDispatcherDependenciesImpl(
            @Nonnull final ServerSessionManager manager,
            @Nonnull PCEPTopologyConfiguration topologyConfigDependencies
    ) {
        this.manager = requireNonNull(manager);
        this.topologyConfigDependencies = requireNonNull(topologyConfigDependencies);
    }

    @Override
    public InetSocketAddress getAddress() {
        return this.topologyConfigDependencies.getAddress();
    }

    @Override
    public KeyMapping getKeys() {
        return this.topologyConfigDependencies.getKeys();
    }

    @Nonnull
    @Override
    public SpeakerIdMapping getSpeakerIdMapping() {
        return this.topologyConfigDependencies.getSpeakerIds();
    }

    @Override
    public PCEPSessionListenerFactory getListenerFactory() {
        return this.manager;
    }

    @Override
    public PCEPPeerProposal getPeerProposal() {
        return this.manager;
    }
}
