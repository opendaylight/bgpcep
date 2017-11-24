/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PCEPStatefulCapabilityTest {

    private static final Tlvs EXPECTED_TLVS =
        new TlvsBuilder().addAugmentation(
            Tlvs1.class, new Tlvs1Builder()
                .setStateful( new StatefulBuilder().setLspUpdateCapability(true)
                    .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(true).build())
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1Builder()
                        .setTriggeredInitialSync(true)
                        .setTriggeredResync(false)
                        .setDeltaLspSyncCapability(true)
                        .setIncludeDbVersion(true)
                        .build())
                    .build())
                .build())
            .build();

    @Test
    public void testPCEPStatefulCapability() {
        final PCEPStatefulCapability sspf = new PCEPStatefulCapability(true, true, true, true, false, true, false);
        Assert.assertTrue(sspf.isActive());
        Assert.assertTrue(sspf.isInstant());
        Assert.assertTrue(sspf.isStateful());
        Assert.assertFalse(sspf.isTriggeredResync());
        Assert.assertTrue(sspf.isTriggeredSync());
        Assert.assertTrue(sspf.isDeltaLspSync());
        Assert.assertTrue(sspf.isIncludeDbVersion());
        final TlvsBuilder builder = new TlvsBuilder();
        sspf.setCapabilityProposal(null, builder);
        Assert.assertEquals(EXPECTED_TLVS, builder.build());
    }
}
