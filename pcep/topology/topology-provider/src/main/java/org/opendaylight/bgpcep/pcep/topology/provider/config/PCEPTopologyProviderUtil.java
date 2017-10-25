/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Client;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;

public final class PCEPTopologyProviderUtil {
    private PCEPTopologyProviderUtil() {
        throw new UnsupportedOperationException();
    }

    public static KeyMapping contructKeys(final List<Client> clients) {
        final KeyMapping ret = KeyMapping.getKeyMapping();

        if (clients != null) {
            clients.stream().filter(client -> client != null && client.getPassword() != null &&
                !client.getPassword().getValue().isEmpty())
                .forEach(mr -> {
                    final Rfc2385Key rfc2385KeyPassword = mr.getPassword();
                    ret.put(IetfInetUtil.INSTANCE.inetAddressFor(mr.getAddress()),
                        rfc2385KeyPassword.getValue().getBytes(StandardCharsets.US_ASCII));
                });
        }
        return ret;
    }
}
