/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapability;

public final class AsNumberUtil {

    private AsNumberUtil() {
        throw new UnsupportedOperationException("Utility class should never be instantiated");
    }

    /**
     * Looks for As4Byte Capability in capabilities and extracts AS number.
     *
     * @param open remote BGP open message
     * @return AsNumber
     */
    public static AsNumber advertizedAsNumber(final Open open) {
        // Look for AS4 capability very defensively
        final List<BgpParameters> params = open.getBgpParameters();
        if (params != null) {
            for (final BgpParameters p : params) {
                final CParameters cp = p.getCParameters();
                if (cp instanceof As4BytesCase) {
                    final As4BytesCapability capa = ((As4BytesCase) cp).getAs4BytesCapability();
                    if (capa != null) {
                        return capa.getAsNumber();
                    }
                }
            }
        }
        // Fallback to whatever is in the header
        return new AsNumber(open.getMyAsNumber().longValue());
    }
}
