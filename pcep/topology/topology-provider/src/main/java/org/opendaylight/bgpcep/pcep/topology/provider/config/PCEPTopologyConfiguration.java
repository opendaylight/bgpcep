/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.SpeakerIdMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev200120.pcep.config.SessionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.config.rev181109.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.sync.optimizations.config.rev181109.PcepNodeSyncConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class PCEPTopologyConfiguration {
    private final InetSocketAddress address;
    private final KeyMapping keys;
    private final TopologyId topologyId;
    private final short rpcTimeout;
    private final @NonNull SpeakerIdMapping speakerIds;
    private final InstanceIdentifier<Topology> topology;

    public PCEPTopologyConfiguration(final @NonNull SessionConfig config, final @NonNull Topology topology) {
        requireNonNull(topology);
        address = PCEPTopologyProviderUtil.getInetSocketAddress(requireNonNull(config.getListenAddress()),
                requireNonNull(config.getListenPort()));
        keys = requireNonNull(PCEPTopologyProviderUtil.contructKeys(topology));
        speakerIds = contructSpeakersId(topology.getNode());
        topologyId = requireNonNull(topology.getTopologyId());
        rpcTimeout = config.getRpcTimeout();
        this.topology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId)).build();
    }

    public @NonNull TopologyId getTopologyId() {
        return topologyId;
    }

    public @NonNull InstanceIdentifier<Topology> getTopology() {
        return topology;
    }

    public short getRpcTimeout() {
        return rpcTimeout;
    }

    public @NonNull InetSocketAddress getAddress() {
        return address;
    }

    public @NonNull KeyMapping getKeys() {
        return keys;
    }

    public @NonNull SpeakerIdMapping getSpeakerIds() {
        return speakerIds;
    }

    private static @NonNull SpeakerIdMapping contructSpeakersId(final @Nullable Map<NodeKey, Node> nodes) {
        if (nodes == null) {
            return SpeakerIdMapping.of();
        }

        final var builder = ImmutableMap.<InetAddress, byte[]>builder();
        for (var node : nodes.values()) {
            if (node != null) {
                final var nodeConfig = node.augmentation(PcepNodeConfig.class);
                if (nodeConfig != null) {
                    final var sessionConfig = nodeConfig.getSessionConfig();
                    if (sessionConfig != null) {
                        final var nodeSyncConfig = sessionConfig.augmentation(PcepNodeSyncConfig.class);
                        if (nodeSyncConfig != null) {
                            final var speakerEntityId = nodeSyncConfig.getSpeakerEntityIdValue();
                            if (speakerEntityId != null) {
                                builder.put(InetAddresses.forString(node.getNodeId().getValue()), speakerEntityId);
                            }
                        }
                    }
                }
            }
        }

        return SpeakerIdMapping.copyOf(builder.build());
    }
}
