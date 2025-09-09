/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcinitiateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCCSessionListenerTest {
    private final List<Message> sendMessages = new ArrayList<>();

    @Mock
    private PCEPSession mockedSession;
    @Mock
    private PCCTunnelManager tunnelManager;

    @Before
    public void setup() {
        doAnswer(invocation -> {
            PCCSessionListenerTest.this.sendMessages.add((Message) invocation.getArguments()[0]);
            return null;
        }).when(mockedSession).sendMessage(any());
    }

    @After
    public void cleanup() {
        sendMessages.clear();
    }

    @Test
    public void testOnMessage() {
        final PCCSessionListener listener = new PCCSessionListener(1, tunnelManager, false);
        listener.onMessage(mockedSession, createUpdMsg(true));
        verify(tunnelManager).onMessagePcupd(any(Updates.class), any(PCCSession.class));
        verify(tunnelManager, never()).onMessagePcInitiate(any(Requests.class), any(PCCSession.class));
        listener.onMessage(mockedSession, createUpdMsg(false));
        verify(tunnelManager, times(2)).onMessagePcupd(any(Updates.class), any(PCCSession.class));
        verify(tunnelManager, never()).onMessagePcInitiate(any(Requests.class), any(PCCSession.class));
        listener.onMessage(mockedSession, createInitMsg(false, true));
        verify(tunnelManager, times(2)).onMessagePcupd(any(Updates.class), any(PCCSession.class));
        verify(tunnelManager).onMessagePcInitiate(any(Requests.class), any(PCCSession.class));
        listener.onMessage(mockedSession, createInitMsg(true, false));
        verify(tunnelManager, times(2)).onMessagePcupd(any(Updates.class), any(PCCSession.class));
        verify(tunnelManager, times(2)).onMessagePcInitiate(any(Requests.class), any(PCCSession.class));
        listener.onMessage(mockedSession, createInitMsg(false, false));
        verify(tunnelManager, times(2)).onMessagePcupd(any(Updates.class), any(PCCSession.class));
        verify(tunnelManager, times(3)).onMessagePcInitiate(any(Requests.class), any(PCCSession.class));
    }

    @Test
    public void testOnMessageErrorMode() {
        final PCCSessionListener listener = new PCCSessionListener(1, tunnelManager, true);
        listener.onMessage(mockedSession, createUpdMsg(true));
        verify(mockedSession).sendMessage(any(Message.class));
    }

    @Test
    public void testOnSessionUp() {
        final PCCSessionListener listener = new PCCSessionListener(1, tunnelManager, false);
        listener.onSessionUp(mockedSession);
        verify(tunnelManager).onSessionUp(any(PCCSession.class));
    }

    @Test
    public void testOnSessionDown() {
        final PCCSessionListener listener = new PCCSessionListener(1, tunnelManager, false);
        listener.onSessionDown(mockedSession, new Exception());
        verify(tunnelManager).onSessionDown(any(PCCSession.class));
    }

    @Test
    public void testSendError() {
        final PCCSessionListener listener = new PCCSessionListener(1, tunnelManager, false);
        listener.onSessionUp(mockedSession);
        listener.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.ATTEMPT_2ND_SESSION, Uint32.ZERO));
        verify(mockedSession).sendMessage(any());
    }

    @Test
    public void testSendReport() {
        final PCCSessionListener listener = new PCCSessionListener(1, tunnelManager, false);
        listener.onSessionUp(mockedSession);
        listener.sendReport(null);
        verify(mockedSession).sendMessage(any());
    }

    @Test
    public void testGetId() {
        final PCCSessionListener listener = new PCCSessionListener(1, tunnelManager, false);
        assertEquals(1, listener.getId());
    }

    private static PcinitiateMessage createInitMsg(final boolean remove, final boolean endpoint) {
        final RequestsBuilder reqBuilder = new RequestsBuilder()
            // lsp with "unknown" plsp-id
            .setLsp(new LspBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setPlspId(new PlspId(Uint32.valueOf(999)))
                .setLspFlags(new LspFlagsBuilder()
                    .setAdministrative(true)
                    .setDelegate(true)
                    .setOperational(OperationalStatus.Up)
                    .setRemove(remove)
                    .setSync(true)
                    .build())
                .build())
            .setSrp(new SrpBuilder(MsgBuilderUtil.createSrp(Uint32.valueOf(123))).setRemove(remove).build());
        if (endpoint) {
            reqBuilder.setEndpointsObj(new EndpointsObjBuilder().build());
        }

        return new PcinitiateBuilder()
            .setPcinitiateMessage(new PcinitiateMessageBuilder().setRequests(List.of(reqBuilder.build())).build())
            .build();
    }

    private static Pcupd createUpdMsg(final boolean delegation) {
        return new PcupdBuilder()
            .setPcupdMessage(new PcupdMessageBuilder()
                .setUpdates(List.of(new UpdatesBuilder()
                    .setLsp(new LspBuilder().setLspFlags(new LspFlagsBuilder().setDelegate(delegation).build())
                        .setPlspId(new PlspId(Uint32.ONE)).build())
                    .setPath(new PathBuilder()
                        .setEro(new EroBuilder()
                            .setSubobject(List.of(new SubobjectBuilder()
                                .setSubobjectType(new IpPrefixCaseBuilder()
                                    .setIpPrefix(new IpPrefixBuilder()
                                        .setIpPrefix(new IpPrefix(new Ipv4Prefix("127.0.0.2/32")))
                                        .build())
                                    .build())
                                .build()))
                            .build())
                        .build())
                    .setSrp(new SrpBuilder().setOperationId(new SrpIdNumber(Uint32.ZERO)).build())
                    .build()))
                .build())
            .build();
    }
}
