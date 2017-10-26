/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.PcinitiateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

public class PCCSessionListenerTest {

    @Mock
    private PCEPSession mockedSession;

    @Mock
    private PCCTunnelManager tunnelManager;

    private final List<Message> sendMessages = Lists.newArrayList();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(invocation -> {
            PCCSessionListenerTest.this.sendMessages.add((Message) invocation.getArguments()[0]);
            return null;
        }).when(this.mockedSession).sendMessage(Mockito.any(Message.class));
    }

    @After
    public void cleanup() {
        this.sendMessages.clear();
    }

    @Test
    public void testOnMessage() {
        final PCCSessionListener listener = new PCCSessionListener(1, this.tunnelManager, false);
        listener.onMessage(this.mockedSession, createUpdMsg(true));
        verify(this.tunnelManager).onMessagePcupd(Mockito.any(Updates.class), Mockito.any(PCCSession.class));
        verify(this.tunnelManager, Mockito.never())
            .onMessagePcInitiate(Mockito.any(Requests.class), Mockito.any(PCCSession.class));
        listener.onMessage(this.mockedSession, createUpdMsg(false));
        verify(this.tunnelManager, Mockito.times(2))
            .onMessagePcupd(Mockito.any(Updates.class), Mockito.any(PCCSession.class));
        verify(this.tunnelManager, Mockito.never())
            .onMessagePcInitiate(Mockito.any(Requests.class), Mockito.any(PCCSession.class));
        listener.onMessage(this.mockedSession, createInitMsg(false, true));
        verify(this.tunnelManager, Mockito.times(2))
            .onMessagePcupd(Mockito.any(Updates.class), Mockito.any(PCCSession.class));
        verify(this.tunnelManager).onMessagePcInitiate(Mockito.any(Requests.class), Mockito.any(PCCSession.class));
        listener.onMessage(this.mockedSession, createInitMsg(true, false));
        verify(this.tunnelManager, Mockito.times(2))
            .onMessagePcupd(Mockito.any(Updates.class), Mockito.any(PCCSession.class));
        verify(this.tunnelManager, Mockito.times(2))
            .onMessagePcInitiate(Mockito.any(Requests.class), Mockito.any(PCCSession.class));
        listener.onMessage(this.mockedSession, createInitMsg(false, false));
        verify(this.tunnelManager, Mockito.times(2))
            .onMessagePcupd(Mockito.any(Updates.class), Mockito.any(PCCSession.class));
        verify(this.tunnelManager, Mockito.times(3))
            .onMessagePcInitiate(Mockito.any(Requests.class), Mockito.any(PCCSession.class));
    }

    @Test
    public void testOnMessageErrorMode() {
        final PCCSessionListener listener = new PCCSessionListener(1, this.tunnelManager, true);
        listener.onMessage(this.mockedSession, createUpdMsg(true));
        verify(this.mockedSession).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testOnSessionUp() {
        final PCCSessionListener listener = new PCCSessionListener(1, this.tunnelManager, false);
        listener.onSessionUp(this.mockedSession);
        verify(this.tunnelManager).onSessionUp(Mockito.any(PCCSession.class));
    }

    @Test
    public void testOnSessionDown() {
        final PCCSessionListener listener = new PCCSessionListener(1, this.tunnelManager, false);
        listener.onSessionDown(this.mockedSession, new Exception());
        verify(this.tunnelManager).onSessionDown(Mockito.any(PCCSession.class));
    }

    @Test
    public void testSendError() {
        final PCCSessionListener listener = new PCCSessionListener(1, this.tunnelManager, false);
        listener.onSessionUp(this.mockedSession);
        listener.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.ATTEMPT_2ND_SESSION, 0));
        verify(this.mockedSession).sendMessage(Mockito.any());
    }

    @Test
    public void testSendReport() {
        final PCCSessionListener listener = new PCCSessionListener(1, this.tunnelManager, false);
        listener.onSessionUp(this.mockedSession);
        listener.sendReport(null);
        verify(this.mockedSession).sendMessage(Mockito.any());
    }

    @Test
    public void testGetId() {
        final PCCSessionListener listener = new PCCSessionListener(1, this.tunnelManager, false);
        Assert.assertEquals(1, listener.getId());
    }

    private static PcinitiateMessage createInitMsg(final boolean remove, final boolean endpoint) {
        // lsp with "unknown" plsp-id
        final LspBuilder lspBuilder = new LspBuilder()
            .setAdministrative(true)
            .setDelegate(true)
            .setIgnore(false)
            .setOperational(OperationalStatus.Up)
            .setPlspId(new PlspId(999L))
            .setProcessingRule(false)
            .setRemove(remove)
            .setSync(true);

        final List<Requests> requests = Lists.newArrayList();
        final RequestsBuilder reqBuilder = new RequestsBuilder()
            .setLsp(lspBuilder.build())
            .setSrp(new SrpBuilder(MsgBuilderUtil.createSrp(123)).addAugmentation(Srp1.class,
                new Srp1Builder().setRemove(remove).build()).build());
        if (endpoint) {
            reqBuilder.setEndpointsObj(new EndpointsObjBuilder().build());
        }
        requests.add(reqBuilder.build());

        final PcinitiateMessageBuilder initBuilder = new PcinitiateMessageBuilder().setRequests(requests);
        return new PcinitiateBuilder().setPcinitiateMessage(initBuilder.build()).build();
    }

    private static Pcupd createUpdMsg(final boolean delegation) {
        final PcupdMessageBuilder msgBuilder = new PcupdMessageBuilder();
        final UpdatesBuilder updsBuilder = new UpdatesBuilder();
        updsBuilder.setLsp(new LspBuilder().setDelegate(delegation).setPlspId(new PlspId(1L)).build());
        final PathBuilder pathBuilder = new PathBuilder();
        pathBuilder.setEro(new EroBuilder().setSubobject(Lists.newArrayList(new SubobjectBuilder()
            .setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder()
                .setIpPrefix(new IpPrefix(new Ipv4Prefix("127.0.0.2/32"))).build()).build()).build())).build());
        updsBuilder.setPath(pathBuilder.build());
        updsBuilder.setSrp(new SrpBuilder().setOperationId(new SrpIdNumber(0L)).build());
        msgBuilder.setUpdates(Lists.newArrayList(updsBuilder.build()));
        return new PcupdBuilder().setPcupdMessage(msgBuilder.build()).build();
    }
}
