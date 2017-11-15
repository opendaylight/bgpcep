/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pce.config.rev171025.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPTopologyProviderUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProviderUtil.class);

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    private PCEPTopologyProviderUtil() {
        throw new UnsupportedOperationException();
    }

    static KeyMapping contructKeys(@Nonnull final Topology topology) {
        final KeyMapping ret = KeyMapping.getKeyMapping();
        topology.getNode().stream()
                .filter(Objects::nonNull)
                .filter(node -> node.getAugmentation(PcepNodeConfig.class) != null)
                .filter(node -> node.getAugmentation(PcepNodeConfig.class).getSessionConfig() != null)
                .filter(node -> !node.getAugmentation(PcepNodeConfig.class)
                        .getSessionConfig().getPassword().getValue().isEmpty())
                .forEach(node -> {
                    final PcepNodeConfig config = node.getAugmentation(PcepNodeConfig.class);
                    final Rfc2385Key rfc2385KeyPassword = config.getSessionConfig().getPassword();
                    final InetAddress address = InetAddresses.forString(node.getNodeId().getValue());
                    ret.put(address, rfc2385KeyPassword.getValue().getBytes(StandardCharsets.US_ASCII));
                });

        return ret;
    }

    static InetSocketAddress getInetSocketAddress(@Nonnull final IpAddress address, @Nonnull final PortNumber port) {
        return new InetSocketAddress(IetfInetUtil.INSTANCE.inetAddressFor(address), port.getValue());
    }

    static boolean filterPcepTopologies(@Nullable final TopologyTypes topologyTypes) {
        if (topologyTypes == null) {
            return false;
        }
        final TopologyTypes1 aug = topologyTypes.getAugmentation(TopologyTypes1.class);

        return aug != null && aug.getTopologyPcep() != null;
    }

    static void closeTopology(@Nullable final PCEPTopologyProviderBean topology, @Nonnull final TopologyId topologyId) {
        if (topology != null) {
            try {
                topology.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (final Exception e) {
                LOG.error("Topology {} instance failed to close service instance", topologyId, e);
            }
            topology.close();
        }
    }
}
