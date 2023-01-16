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
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.ietf.stateful.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCCTriggeredFullDBResyncTest extends PCCMockCommon {

    @Test
    public void testSessionTriggeredFullDBReSync() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final int lspQuantity = 3;
        final Uint64 numberOflspAndDBv = Uint64.valueOf(lspQuantity);
        final Channel channel = createServer(factory, remoteAddress, new PCCPeerProposal());
        final PCEPSession session = createPCCSession(numberOflspAndDBv).get();
        assertNotNull(session);
        final TestingSessionListener pceSessionListener = getListener(factory);
        assertNotNull(pceSessionListener);
        checkSynchronizedSession(lspQuantity, pceSessionListener, numberOflspAndDBv);
        pccSessionListener.onMessage(session, createTriggerLspResync());
        final TestingSessionListener sessionListenerAfterReconnect = getListener(factory);
        checkResyncSession(Optional.of(lspQuantity), 4,8, null, numberOflspAndDBv, sessionListenerAfterReconnect);
        channel.close().get();
    }

    private static Message createTriggerLspResync() {
        final SrpBuilder srpBuilder = new SrpBuilder()
                .setOperationId(new SrpIdNumber(Uint32.ONE))
                .setProcessingRule(Boolean.TRUE);

        final Srp srp = srpBuilder.build();
        final Lsp lsp = new LspBuilder().setPlspId(new PlspId(Uint32.ZERO)).setSync(Boolean.TRUE).build();
        final UpdatesBuilder rb = new UpdatesBuilder()
                .setSrp(srp)
                .setLsp(lsp)
                .setPath(new PathBuilder().build());
        final PcupdMessageBuilder ub = new PcupdMessageBuilder();
        ub.setUpdates(List.of(rb.build()));
        return new PcupdBuilder().setPcupdMessage(ub.build()).build();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        return List.of(new PCEPStatefulCapability(true, true, false, true, false, true));
    }
}
