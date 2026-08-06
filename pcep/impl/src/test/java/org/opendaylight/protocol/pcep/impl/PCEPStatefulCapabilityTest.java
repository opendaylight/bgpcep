/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapabilityBuilder;

class PCEPStatefulCapabilityTest {
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
            .setStatefulCapability(new StatefulCapabilityBuilder()
                .setLspUpdateCapability(true)
                .setInitiation(true)
                .setTriggeredInitialSync(true)
                .setTriggeredResync(false)
                .setDeltaLspSyncCapability(true)
                .setIncludeDbVersion(true)
                .build())
            .build(), builder.build());
    }
}
