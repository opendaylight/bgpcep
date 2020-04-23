/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapability;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

public final class AsNumberUtil {
    private AsNumberUtil() {
        // Hidden on purpose
    }

    /**
     * Looks for As4Byte Capability in capabilities and extracts AS number.
     *
     * @param open remote BGP open message
     * @return AsNumber
     */
    public static @NonNull AsNumber advertizedAsNumber(final Open open) {
        // Look for AS4 capability very defensively
        final List<BgpParameters> params = open.getBgpParameters();
        if (params != null) {
            for (final BgpParameters p : params) {
                for (final OptionalCapabilities oc : p.nonnullOptionalCapabilities()) {
                    final CParameters cparams = oc.getCParameters();
                    if (cparams != null) {
                        final As4BytesCapability as4 = cparams.getAs4BytesCapability();
                        if (as4 != null) {
                            return as4.getAsNumber();
                        }
                    }
                }
            }
        }
        // Fallback to whatever is in the header
        return new AsNumber(Uint32.valueOf(open.getMyAsNumber()));
    }

    /**
     * Extract AS from Container Node.
     *
     * @param dtc route container
     * @param nid as node identifier
     * @return as number
     */
    public static AsNumber extractAS(final DataContainerNode<?> dtc, final NodeIdentifier nid) {
        final NormalizedNode<?, ?> as = NormalizedNodes.findNode(dtc, nid).orElse(null);
        if (as != null) {
            return new AsNumber((Uint32) as.getValue());
        }
        return null;
    }
}
