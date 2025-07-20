/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.PcepSessionTls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.Uint16;

final class TopologyPCEPSessionNegotiatorFactory extends DefaultPCEPSessionNegotiatorFactory {
    private final PCEPStatefulPeerProposal proposal;

    TopologyPCEPSessionNegotiatorFactory(final PCEPSessionListenerFactory listenerFactory,
            final PCEPTimerProposal timers, final List<PCEPCapability> capabilities, final Uint16 maxUnknownMessages,
            final PcepSessionTls tlsConfiguration, final DataBroker dataBroker,
            final WithKey<Topology, TopologyKey> topology) {
        super(listenerFactory, timers, capabilities, maxUnknownMessages, tlsConfiguration);
        proposal = new PCEPStatefulPeerProposal(dataBroker, topology);
    }

    @Override
    protected void appendPeerSpecificTls(final InetSocketAddress address, final TlvsBuilder builder) {
        proposal.setPeerSpecificProposal(address, builder);
    }

    void close() {
        proposal.close();
    }
}
