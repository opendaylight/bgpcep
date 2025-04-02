/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.open.object.open.TlvsBuilder;

public final class PCEPSrv6Capability extends PCEPCapability {
    private static final @NonNull PCEPSrv6Capability INSTANCE = new PCEPSrv6Capability();

    private PCEPSrv6Capability() {
        // Hidden on purpose
    }

    public static @NonNull PCEPSrv6Capability of() {
        return INSTANCE;
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        builder.addAugmentation(new Tlvs1Builder()
            .setSrv6PceCapability(new Srv6PceCapabilityBuilder()
                .setNFlag(Boolean.FALSE)
                .build())
            .build());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }
}
