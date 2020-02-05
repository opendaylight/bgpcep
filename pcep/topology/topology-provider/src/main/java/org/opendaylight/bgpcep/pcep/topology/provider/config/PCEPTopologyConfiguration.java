/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static java.util.Objects.requireNonNull;

import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.SpeakerIdMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev200120.pcep.config.SessionConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class PCEPTopologyConfiguration {
    private final InetSocketAddress address;
    private final KeyMapping keys;
    private final TopologyId topologyId;
    private final short rpcTimeout;
    private final SpeakerIdMapping speakerIds;
    private final InstanceIdentifier<Topology> topology;

    public PCEPTopologyConfiguration(final @NonNull SessionConfig config, final @NonNull Topology topology) {
        requireNonNull(topology);
        this.address = PCEPTopologyProviderUtil.getInetSocketAddress(requireNonNull(config.getListenAddress()),
                requireNonNull(config.getListenPort()));
        this.keys = requireNonNull(PCEPTopologyProviderUtil.contructKeys(topology));
        this.speakerIds = requireNonNull(PCEPTopologyProviderUtil.contructSpeakersId(topology));
        this.topologyId = requireNonNull(topology.getTopologyId());
        this.rpcTimeout = config.getRpcTimeout();
        this.topology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(this.topologyId)).build();
    }

    public @NonNull TopologyId getTopologyId() {
        return this.topologyId;
    }

    public @NonNull InstanceIdentifier<Topology> getTopology() {
        return this.topology;
    }

    public short getRpcTimeout() {
        return this.rpcTimeout;
    }

    public @NonNull InetSocketAddress getAddress() {
        return this.address;
    }

    public @NonNull KeyMapping getKeys() {
        return this.keys;
    }

    public @NonNull SpeakerIdMapping getSpeakerIds() {
        return this.speakerIds;
    }
}
