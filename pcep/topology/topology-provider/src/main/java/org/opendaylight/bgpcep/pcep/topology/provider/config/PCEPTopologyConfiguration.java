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
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgpcep.pcep.topology.provider.SpeakerIdMapping;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev200120.pcep.config.SessionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.config.rev181109.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.config.rev181109.PcepTopologyTypeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.sync.optimizations.config.rev181109.PcepNodeSyncConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public final class PCEPTopologyConfiguration implements Immutable {
    private final @NonNull InetSocketAddress address;
    private final @NonNull KeyMapping keys;
    private final short rpcTimeout;
    private final @NonNull SpeakerIdMapping speakerIds;
    private final @NonNull KeyedInstanceIdentifier<Topology, TopologyKey> topology;

    public PCEPTopologyConfiguration(final @NonNull Topology topology) {
        this.topology = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topology.key());

        final SessionConfig config = topology.augmentation(PcepTopologyTypeConfig.class).getSessionConfig();
        address = getInetSocketAddress(config.getListenAddress(), config.getListenPort());
        keys = constructKeys(topology.getNode());
        speakerIds = contructSpeakersId(topology.getNode());
        rpcTimeout = config.getRpcTimeout();
    }

    public @NonNull TopologyId getTopologyId() {
        return topology.getKey().getTopologyId();
    }

    public @NonNull KeyedInstanceIdentifier<Topology, TopologyKey> getTopology() {
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

    private static @NonNull KeyMapping constructKeys(final @Nullable Map<NodeKey, Node> nodes) {
        if (nodes == null) {
            return KeyMapping.of();
        }

        final var passwords = new HashMap<InetAddress, String>();
        for (var node : nodes.values()) {
            if (node != null) {
                final var nodeConfig = node.augmentation(PcepNodeConfig.class);
                if (nodeConfig != null) {
                    final var sessionConfig = nodeConfig.getSessionConfig();
                    if (sessionConfig != null) {
                        final var rfc2385KeyPassword = sessionConfig.getPassword();
                        if (rfc2385KeyPassword != null) {
                            final String password = rfc2385KeyPassword.getValue();
                            if (!password.isEmpty()) {
                                passwords.put(nodeAddress(node), password);
                            }
                        }
                    }
                }
            }
        }

        return KeyMapping.of(passwords);
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
                                builder.put(nodeAddress(node), speakerEntityId);
                            }
                        }
                    }
                }
            }
        }

        return SpeakerIdMapping.copyOf(builder.build());
    }

    private static InetAddress nodeAddress(final Node node) {
        return InetAddresses.forString(node.getNodeId().getValue());
    }

    private static @NonNull InetSocketAddress getInetSocketAddress(final IpAddressNoZone address,
            final PortNumber port) {
        return new InetSocketAddress(IetfInetUtil.INSTANCE.inetAddressForNoZone(requireNonNull(address)),
            port.getValue().toJava());
    }
}
