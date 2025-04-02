/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.open.object.open.TlvsBuilder;

public class PCEPSrv6CapabilityTest {
    @Test
    public void testSegmentRoutingCapability() {
        final var builder = new TlvsBuilder();
        PCEPSrv6Capability.of().setCapabilityProposal(null, builder);
        assertEquals(new TlvsBuilder()
            .addAugmentation(new Tlvs1Builder()
                .setSrv6PceCapability(new Srv6PceCapabilityBuilder()
                    .setNFlag(Boolean.FALSE)
                    .build())
                .build())
            .build(), builder.build());
    }
}
