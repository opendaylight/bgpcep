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
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.impl.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.SrpIdNumber;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCCTriggeredSyncTest extends PCCMockCommon {
    @Test
    public void testSessionTriggeredSync() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, remoteAddress, new PCCPeerProposal());
        final Uint64 numberOflspAndDBv = Uint64.valueOf(3);
        final PCEPSession session = createPCCSession(numberOflspAndDBv).get();
        assertNotNull(session);
        final TestingSessionListener pceSessionListener = getListener(factory);
        assertNotNull(pceSessionListener);
        checkSynchronizedSession(0, pceSessionListener, Uint64.ZERO);
        pccSessionListener.onMessage(session, createTriggerMsg());
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
            .setLsp(new LspBuilder().setPlspId(new PlspId(Uint32.ZERO))
                .setLspFlags(new LspFlagsBuilder().setSync(Boolean.TRUE).build()).build())
            .setPath(new PathBuilder().build());
        final PcupdMessageBuilder ub = new PcupdMessageBuilder();
        ub.setUpdates(List.of(rb.build()));
        return new PcupdBuilder().setPcupdMessage(ub.build()).build();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        return List.of(new PCEPStatefulCapability(true, true, true, false, false, true));
    }
}

