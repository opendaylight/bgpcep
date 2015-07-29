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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class SegmentRouting02SessionProposalTest {

    private static final Tlvs EXPECTED_TLVS =
        new TlvsBuilder()
            .addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder()
                .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(true).build())
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1Builder()
                    .setTriggeredInitialSync(true)
                    .setTriggeredResync(false)
                    .setDeltaLspSyncCapability(true)
                    .setIncludeDbVersion(false)
                    .build())
                .setLspUpdateCapability(true).build()).build())
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.Tlvs1.class,
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.Tlvs1Builder()
                    .setSrPceCapability(new SrPceCapabilityBuilder().setMsd((short) 0).build()).build())
            .build();

    @Test
    public void testSegmentRouting02SessionProposalFactory() {
        final SegmentRoutingSessionProposalFactory sspf = new SegmentRoutingSessionProposalFactory(true, true, true, true, true, false, true, false);
        Assert.assertTrue(sspf.isActive());
        Assert.assertTrue(sspf.isInstant());
        Assert.assertTrue(sspf.isStateful());
        Assert.assertTrue(sspf.isSegmentRoutingCapable());
        Assert.assertFalse(sspf.isTriggeredResync());
        Assert.assertTrue(sspf.isTriggeredSync());
        Assert.assertTrue(sspf.isDeltaLspSync());
        Assert.assertFalse(sspf.isIncludeDbVersion());
        final TlvsBuilder builder = new TlvsBuilder();
        sspf.setCapabilityProposal(null, builder);
        Assert.assertEquals(EXPECTED_TLVS, builder.build());
    }
}
