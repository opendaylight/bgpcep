/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPSegmentRoutingCapabilityTest {
    private static final Tlvs EXPECTED_TLVS =
        new TlvsBuilder().addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep
                .segment.routing.rev181109.Tlvs1Builder().setSrPceCapability(new SrPceCapabilityBuilder()
                    .setMsd(Uint8.ZERO).build()).build()).build();

    @Test
    public void testSegmentRoutingCapability() {
        final PCEPSegmentRoutingCapability sspf = new PCEPSegmentRoutingCapability(true);
        Assert.assertTrue(sspf.isSegmentRoutingCapable());
        final TlvsBuilder builder = new TlvsBuilder();
        sspf.setCapabilityProposal(null, builder);
        Assert.assertEquals(EXPECTED_TLVS, builder.build());
    }
}
