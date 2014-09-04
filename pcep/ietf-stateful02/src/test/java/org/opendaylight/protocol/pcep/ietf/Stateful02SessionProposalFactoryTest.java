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
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02SessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class Stateful02SessionProposalFactoryTest {

    private static final int DEAD_TIMER = 4;
    private static final int KEEP_ALIVE = 1;
    private static final int TIMEOUT = 100;
    private static final int SESSION_ID = 1;
    private static final Open OPEN_MSG = new OpenBuilder()
            .setDeadTimer((short) DEAD_TIMER)
            .setKeepalive((short) KEEP_ALIVE)
            .setSessionId((short) SESSION_ID)
            .setTlvs(new TlvsBuilder()
                .addAugmentation(Tlvs1.class, new Tlvs1Builder().setLspCleanup(new LspCleanupBuilder().setTimeout((long) TIMEOUT).build()).build())
                .addAugmentation(Tlvs2.class, new Tlvs2Builder().setStateful(new StatefulBuilder().addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(true).build()).setLspUpdateCapability(true).build()).build())
                .build())
            .build();

    @Test
    public void testStateful02SessionProposalFactory() {
        final Stateful02SessionProposalFactory sspf = new Stateful02SessionProposalFactory(DEAD_TIMER, KEEP_ALIVE, true, true, true, TIMEOUT);
        Assert.assertEquals(DEAD_TIMER, sspf.getDeadTimer());
        Assert.assertEquals(KEEP_ALIVE, sspf.getKeepAlive());
        Assert.assertTrue(sspf.isActive());
        Assert.assertTrue(sspf.isInstant());
        Assert.assertTrue(sspf.isStateful());
        Assert.assertEquals(OPEN_MSG, sspf.getSessionProposal(null, SESSION_ID));
    }

}
