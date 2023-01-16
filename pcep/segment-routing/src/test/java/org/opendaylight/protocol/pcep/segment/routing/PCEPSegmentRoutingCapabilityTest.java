/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPSegmentRoutingCapabilityTest {
    @Test
    public void testSegmentRoutingCapability() {
        final var builder = new TlvsBuilder();
        PCEPSegmentRoutingCapability.of().setCapabilityProposal(null, builder);
        assertEquals(new TlvsBuilder()
            .addAugmentation(new Tlvs1Builder()
                .setSrPceCapability(new SrPceCapabilityBuilder()
                    .setNFlag(Boolean.FALSE).setXFlag(Boolean.FALSE).setMsd(Uint8.ZERO)
                    .build())
                .build())
            .build(), builder.build());
    }
}
