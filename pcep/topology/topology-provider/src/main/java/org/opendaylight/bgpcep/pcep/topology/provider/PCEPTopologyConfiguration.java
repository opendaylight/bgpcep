/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPTimerProposal;
import org.opendaylight.protocol.pcep.ietf.stateful.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.impl.PCEPAssociationCapability;
import org.opendaylight.protocol.pcep.impl.PCEPPathSetupTypeCapability;
import org.opendaylight.protocol.pcep.p2mp.te.lsp.P2MPTeLspCapability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.stats.provider.config.rev220730.PcepTopologyNodeStatsProviderAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.rev230115.TopologyPcep1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.rev230115.network.topology.topology.topology.types.topology.pcep.Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.PcepSessionTls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.common.Uint16;

final class PCEPTopologyConfiguration implements Immutable {
    private static final long DEFAULT_UPDATE_INTERVAL = TimeUnit.SECONDS.toNanos(5);
    private static final @NonNull ImmutableList<PCEPCapability> DEFAULT_CAPABILITIES = ImmutableList.of(
        new PCEPStatefulCapability(),
        new PCEPAssociationCapability(),
        new PCEPPathSetupTypeCapability(),
        P2MPTeLspCapability.of());

    private final @NonNull InetSocketAddress address;
    private final @Nullable GraphKey graphKey;
    private final @NonNull KeyMapping keys;
    private final @NonNull PCEPTimerProposal timerProposal;
    private final @NonNull ImmutableList<PCEPCapability> capabilities;
    private final @NonNull Uint16 maxUnknownMessages;
    private final @Nullable PcepSessionTls tls;
    private final long updateIntervalNanos;
    private final short rpcTimeout;

    PCEPTopologyConfiguration(final InetSocketAddress address, final KeyMapping keys, final @Nullable GraphKey graphKey,
            final short rpcTimeout, final long updateIntervalNanos, final PCEPTimerProposal timerProposal,
            final @NonNull ImmutableList<PCEPCapability> capabilities, final Uint16 maxUnknownMessages,
            final @Nullable PcepSessionTls tls) {
        this.address = requireNonNull(address);
        this.keys = requireNonNull(keys);
        this.graphKey = graphKey;
        this.rpcTimeout = rpcTimeout;
        this.updateIntervalNanos = updateIntervalNanos;
        this.timerProposal = requireNonNull(timerProposal);
        this.maxUnknownMessages = requireNonNull(maxUnknownMessages);
        this.tls = tls;
        this.capabilities = requireNonNull(capabilities);
    }

    static @Nullable PCEPTopologyConfiguration of(final @NonNull Topology topology) {
        final var types = topology.getTopologyTypes();
        if (types == null) {
            return null;
        }
        final var typesAug = types.augmentation(TopologyTypes1.class);
        if (typesAug == null) {
            return null;
        }
        final var topologyPcep = typesAug.getTopologyPcep();
        if (topologyPcep == null) {
            return null;
        }
        final var sessionConfig = topologyPcep.getSessionConfig();
        if (sessionConfig == null) {
            return null;
        }
        final var capabilityAug = topologyPcep.augmentation(TopologyPcep1.class);
        final var capabilities = capabilityAug != null ? capabilityAug.getCapabilities() : null;

        if (capabilities != null && !capabilities.nonnullStateful().requireEnabled()) {
            return null;
        }

        final var updateAug = topologyPcep.augmentation(PcepTopologyNodeStatsProviderAug.class);
        final long updateInterval = updateAug != null ? TimeUnit.SECONDS.toNanos(updateAug.requireTimer().toJava())
            : DEFAULT_UPDATE_INTERVAL;

        return new PCEPTopologyConfiguration(
            getInetSocketAddress(sessionConfig.getListenAddress(), sessionConfig.getListenPort()),
            constructKeys(topology.getNode()), constructGraphKey(topologyPcep.getTedName()),
            sessionConfig.getRpcTimeout(), updateInterval, new PCEPTimerProposal(sessionConfig),
            constructCapabilities(capabilities), sessionConfig.requireMaxUnknownMessages(), sessionConfig.getTls());
    }

    private static @NonNull ImmutableList<PCEPCapability> constructCapabilities(final Capabilities capabilities) {
        if (capabilities == null) {
            return DEFAULT_CAPABILITIES;
        }

        final var builder = ImmutableList.<PCEPCapability>builder()
            .add(new PCEPStatefulCapability(capabilities.nonnullStateful()))
            .add(new PCEPAssociationCapability(capabilities.nonnullAssociationGroup()))
            .add(new PCEPPathSetupTypeCapability(capabilities.nonnullPathSetupType()));
        if (capabilities.nonnullP2mp().requireEnabled()) {
            builder.add(P2MPTeLspCapability.of());
        }
        return builder.build();
    }

    short getRpcTimeout() {
        return rpcTimeout;
    }

    long getUpdateInterval() {
        return updateIntervalNanos;
    }

    @NonNull InetSocketAddress getAddress() {
        return address;
    }

    @NonNull KeyMapping getKeys() {
        return keys;
    }

    @Nullable GraphKey getGraphKey() {
        return graphKey;
    }

    @NonNull PCEPTimerProposal getTimerProposal() {
        return timerProposal;
    }

    /**
     * Returns list containing PCEP Capabilities.
     *
     * @return PCEPCapabilities
     */
    @NonNull ImmutableList<PCEPCapability> getCapabilities() {
        return capabilities;
    }

    @NonNull Uint16 getMaxUnknownMessages() {
        return maxUnknownMessages;
    }

    @Nullable PcepSessionTls getTls() {
        return tls;
    }

    private static @NonNull KeyMapping constructKeys(final @Nullable Map<NodeKey, Node> nodes) {
        if (nodes == null) {
            return KeyMapping.of();
        }

        final var passwords = new HashMap<InetAddress, String>();
        for (var node : nodes.values()) {
            if (node != null) {
                final var nodeConfig = node.augmentation(Node1.class);
                if (nodeConfig != null) {
                    final var sessionConfig = nodeConfig.getSessionConfig();
                    if (sessionConfig != null) {
                        final var rfc2385KeyPassword = sessionConfig.getPassword();
                        if (rfc2385KeyPassword != null) {
                            final var password = rfc2385KeyPassword.getValue();
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

    private static @Nullable GraphKey constructGraphKey(final @Nullable TopologyId topologyId) {
        return topologyId != null ? new GraphKey("ted://" + topologyId.getValue()) : null;
    }

    private static InetAddress nodeAddress(final Node node) {
        return InetAddresses.forString(node.getNodeId().getValue());
    }

    private static @NonNull InetSocketAddress getInetSocketAddress(final IpAddressNoZone address,
            final PortNumber port) {
        return new InetSocketAddress(IetfInetUtil.inetAddressForNoZone(requireNonNull(address)),
            port.getValue().toJava());
    }
}
