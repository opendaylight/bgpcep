/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.app;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.bmp.monitor.config.MonitoredRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KeyConstructorUtil {
    private static final Logger LOG = LoggerFactory.getLogger(KeyConstructorUtil.class);

    private KeyConstructorUtil() {
        throw new UnsupportedOperationException();
    }

    public static Optional<KeyMapping> constructKeys(final List<MonitoredRouter> mrs) {
        final KeyMapping ret = KeyMapping.getKeyMapping();
        if (mrs != null) {
            for (final MonitoredRouter mr : mrs) {
                if (mr.getAddress() == null) {
                    LOG.warn("Monitored router {} does not have an address skipping it", mr);
                    continue;
                }
                final Rfc2385Key rfc2385KeyPassword = mr.getPassword();
                if (rfc2385KeyPassword != null && !rfc2385KeyPassword.getValue().isEmpty()) {
                    final String s = Ipv4Util.toStringIP(mr.getAddress());
                    ret.put(InetAddresses.forString(s), rfc2385KeyPassword.getValue().getBytes(StandardCharsets.US_ASCII));
                }
            }
        }

        return ret.isEmpty() ? Optional.absent() : Optional.of(ret);
    }
}
