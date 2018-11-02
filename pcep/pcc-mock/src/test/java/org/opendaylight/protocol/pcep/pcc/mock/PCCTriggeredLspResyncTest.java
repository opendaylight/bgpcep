/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertNotNull;

import io.netty.channel.Channel;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

public class PCCTriggeredLspResyncTest extends PCCMockCommon {
    @Test
    public void testSessionTriggeredLspReSync() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final int lspQuantity = 3;
        final BigInteger numberOflspAndDBv = BigInteger.valueOf(lspQuantity);

        final Channel channel = createServer(factory, this.remoteAddress, new PCCPeerProposal());
        final PCEPSession session = createPCCSession(numberOflspAndDBv).get();
        assertNotNull(session);
        final TestingSessionListener pceSessionListener = getListener(factory);
        assertNotNull(pceSessionListener);
        checkSynchronizedSession(lspQuantity, pceSessionListener, numberOflspAndDBv);
        this.pccSessionListener.onMessage(session, createTriggerLspResync());
        final TestingSessionListener sessionListenerAfterReconnect = getListener(factory);
        checkResyncSession(Optional.of(lspQuantity), 2, 6, null, numberOflspAndDBv, sessionListenerAfterReconnect);
        channel.close().get();
    }

    private static Message createTriggerLspResync() {
        final SrpBuilder srpBuilder = new SrpBuilder();
        srpBuilder.setOperationId(new SrpIdNumber(1L));
        srpBuilder.setProcessingRule(Boolean.TRUE);

        final Srp srp = srpBuilder.build();
        final Lsp lsp = new LspBuilder().setPlspId(new PlspId(2L)).setSync(Boolean.TRUE).build();
        final UpdatesBuilder rb = new UpdatesBuilder();
        rb.setSrp(srp);
        rb.setLsp(lsp);
        final PathBuilder pb = new PathBuilder();
        rb.setPath(pb.build());
        final PcupdMessageBuilder ub = new PcupdMessageBuilder();
        ub.setUpdates(Collections.singletonList(rb.build()));
        return new PcupdBuilder().setPcupdMessage(ub.build()).build();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(true, true, true, false, true, false, true));
        return caps;
    }
}
