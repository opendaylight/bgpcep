/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class SegmentRouting02SessionProposalTest {

    private static final int DEAD_TIMER = 4;
    private static final int KEEP_ALIVE = 1;
    private static final int SESSION_ID = 1;
    private static final Open OPEN_MSG = new OpenBuilder()
            .setDeadTimer((short) DEAD_TIMER)
            .setKeepalive((short) KEEP_ALIVE)
            .setSessionId((short) SESSION_ID)
            .setTlvs(new TlvsBuilder()
                .addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder().addAugmentation(Stateful1.class,
                        new Stateful1Builder().setInitiation(true).build()).setLspUpdateCapability(true).build()).build())
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.Tlvs1.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.Tlvs1Builder()
                        .setSrPceCapability(new SrPceCapabilityBuilder().setMsd((short) 0).build()).build())
                .build())
            .build();

    @Test
    public void testSegmentRouting02SessionProposalFactory() {
        final SegmentRouting02SessionProposalFactory sspf = new SegmentRouting02SessionProposalFactory(DEAD_TIMER, KEEP_ALIVE, true, true, true, true);
        Assert.assertEquals(DEAD_TIMER, sspf.getDeadTimer());
        Assert.assertEquals(KEEP_ALIVE, sspf.getKeepAlive());
        Assert.assertTrue(sspf.isActive());
        Assert.assertTrue(sspf.isInstant());
        Assert.assertTrue(sspf.isStateful());
        Assert.assertTrue(sspf.isSegmentRoutingCapable());
        Assert.assertEquals(OPEN_MSG, sspf.getSessionProposal(null, SESSION_ID));
    }
}
