/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.ietf;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.initiated00.Stateful07SessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class Stateful07SessionProposalFactoryTest {

    private static final int DEAD_TIMER = 4;
    private static final int KEEP_ALIVE = 1;
    private static final int SESSION_ID = 1;
    private static final Open OPEN_MSG = new OpenBuilder()
            .setDeadTimer((short) DEAD_TIMER)
            .setKeepalive((short) KEEP_ALIVE)
            .setSessionId((short) SESSION_ID)
            .setTlvs(new TlvsBuilder()
                .addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder().addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(true).build()).setLspUpdateCapability(true).build()).build())
                .build())
            .build();

    @Test
    public void testStateful07SessionProposalFactory() {
        final Stateful07SessionProposalFactory sspf = new Stateful07SessionProposalFactory(DEAD_TIMER, KEEP_ALIVE, true, true, true);
        Assert.assertEquals(DEAD_TIMER, sspf.getDeadTimer());
        Assert.assertEquals(KEEP_ALIVE, sspf.getKeepAlive());
        Assert.assertTrue(sspf.isActive());
        Assert.assertTrue(sspf.isInstant());
        Assert.assertTrue(sspf.isStateful());
        Assert.assertEquals(OPEN_MSG, sspf.getSessionProposal(null, SESSION_ID));
    }
}
