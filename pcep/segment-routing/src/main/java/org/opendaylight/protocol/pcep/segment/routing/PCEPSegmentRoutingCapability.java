/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class PCEPSegmentRoutingCapability extends PCEPCapability {
    private static final @NonNull PCEPSegmentRoutingCapability INSTANCE = new PCEPSegmentRoutingCapability();

    private PCEPSegmentRoutingCapability() {
        // Hidden on purpose
    }

    public static @NonNull PCEPSegmentRoutingCapability of() {
        return INSTANCE;
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        builder.addAugmentation(new Tlvs1Builder()
            .setSrPceCapability(new SrPceCapabilityBuilder()
                .setNFlag(Boolean.FALSE)
                .setXFlag(Boolean.FALSE)
                .setMsd(Uint8.ZERO)
                .build())
            .build());
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }
}
