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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.ietf.stateful.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCCTriggeredSyncTest extends PCCMockCommon {
    @Test
    public void testSessionTriggeredSync() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, this.remoteAddress, new PCCPeerProposal());
        final Uint64 numberOflspAndDBv = Uint64.valueOf(3);
        final PCEPSession session = createPCCSession(numberOflspAndDBv).get();
        assertNotNull(session);
        final TestingSessionListener pceSessionListener = getListener(factory);
        assertNotNull(pceSessionListener);
        checkSynchronizedSession(0, pceSessionListener, Uint64.ZERO);
        this.pccSessionListener.onMessage(session, createTriggerMsg());
        checkSynchronizedSession(3, pceSessionListener, numberOflspAndDBv);
        channel.close().get();
    }

    private static Message createTriggerMsg() {
        final UpdatesBuilder rb = new UpdatesBuilder()
                // create PCUpd with mandatory objects and LSP object set to 1
                .setSrp(new SrpBuilder()
                    .setIgnore(false)
                    .setProcessingRule(false)
                    .setOperationId(new SrpIdNumber(Uint32.ONE))
                    .build())
                .setLsp(new LspBuilder().setPlspId(new PlspId(Uint32.ZERO)).setSync(Boolean.TRUE).build())
                .setPath(new PathBuilder().build());
        final PcupdMessageBuilder ub = new PcupdMessageBuilder();
        ub.setUpdates(Collections.singletonList(rb.build()));
        return new PcupdBuilder().setPcupdMessage(ub.build()).build();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(true, true, true, true, false, false, true));
        return caps;
    }
}

