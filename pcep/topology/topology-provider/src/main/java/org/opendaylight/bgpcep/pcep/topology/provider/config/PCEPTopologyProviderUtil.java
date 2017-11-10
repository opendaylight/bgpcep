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
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.pcep.topology.config.rev171025.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

final class PCEPTopologyProviderUtil {
    private PCEPTopologyProviderUtil() {
        throw new UnsupportedOperationException();
    }

    static KeyMapping contructKeys(final Topology topology) {
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

    static InetSocketAddress getInetSocketAddress(final IpAddress address, final PortNumber port) {
        final InetAddress inetAddr = IetfInetUtil.INSTANCE.inetAddressFor(address);
        return new InetSocketAddress(inetAddr, port.getValue());
    }
}
