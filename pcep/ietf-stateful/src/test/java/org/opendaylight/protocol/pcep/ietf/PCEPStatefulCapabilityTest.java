/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.stateful.PCEPStatefulCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;

public class PCEPStatefulCapabilityTest {
    @Test
    public void testPCEPStatefulCapability() {
        final PCEPStatefulCapability sspf = new PCEPStatefulCapability(true, true, true, false, true, false);
        assertTrue(sspf.isActive());
        assertTrue(sspf.isInstant());
        assertFalse(sspf.isTriggeredResync());
        assertTrue(sspf.isTriggeredSync());
        assertTrue(sspf.isDeltaLspSync());
        assertTrue(sspf.isIncludeDbVersion());
        final TlvsBuilder builder = new TlvsBuilder();
        sspf.setCapabilityProposal(null, builder);
        assertEquals(new TlvsBuilder()
            .addAugmentation(new Tlvs1Builder()
                .setStateful(new StatefulBuilder().setLspUpdateCapability(true)
                    .addAugmentation(new Stateful1Builder().setInitiation(true).build())
                    .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                        .pcep.sync.optimizations.rev200720.Stateful1Builder()
                            .setTriggeredInitialSync(true)
                            .setTriggeredResync(false)
                            .setDeltaLspSyncCapability(true)
                            .setIncludeDbVersion(true)
                            .build())
                    .build())
                .build())
            .build(), builder.build());
    }
}
