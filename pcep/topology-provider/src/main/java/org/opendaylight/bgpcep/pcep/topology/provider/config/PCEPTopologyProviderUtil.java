/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Client;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTopologyProviderUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProviderUtil.class);

    private PCEPTopologyProviderUtil() {
        throw new UnsupportedOperationException();
    }

    public static Optional<KeyMapping> contructKeys(final List<Client> clients) {
        KeyMapping ret = null;

        if (clients != null && !clients.isEmpty()) {
            ret = KeyMapping.getKeyMapping();
            for (final Client c : clients) {
                if (c.getAddress() == null) {
                    LOG.warn("Client {} does not have an address skipping it", c);
                    continue;
                }
                final Rfc2385Key rfc2385KeyPassword = c.getPassword();
                if (rfc2385KeyPassword != null && !rfc2385KeyPassword.getValue().isEmpty()) {
                    final String s = Ipv4Util.toStringIP(c.getAddress());
                    ret.put(InetAddresses.forString(s), rfc2385KeyPassword.getValue().getBytes(StandardCharsets.US_ASCII));
                }
            }
        }
        return Optional.fromNullable(ret);
    }
}
