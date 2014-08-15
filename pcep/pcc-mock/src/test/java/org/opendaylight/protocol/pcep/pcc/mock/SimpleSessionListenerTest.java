/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;

public class SimpleSessionListenerTest {

    @Mock
    private PCEPSession mockedSession;

    private final List<Message> sendMessages = Lists.newArrayList();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                SimpleSessionListenerTest.this.sendMessages.add((Message) invocation.getArguments()[0]);
                return null;
            }
        }).when(mockedSession).sendMessage(Mockito.any(Message.class));
    }

    @After
    public void cleanup() {
        this.sendMessages.clear();
    }

    @Test
    public void testSessionListenerPcRpt() throws UnknownHostException {
        final SimpleSessionListener sessionListser = new SimpleSessionListener(1, false, 1,
                InetAddress.getByName("127.0.0.1"));

        sessionListser.onSessionUp(this.mockedSession);
        // one lsp + end-of-sync marker
        Mockito.verify(this.mockedSession, Mockito.times(2)).sendMessage(Mockito.any(Message.class));
        assertEquals(2, this.sendMessages.size());
        assertTrue(this.sendMessages.get(0) instanceof Pcrpt);
        assertTrue(this.sendMessages.get(1) instanceof Pcrpt);

        sessionListser.onMessage(this.mockedSession, createUpdMsg());
        // send PcRpt as a response to PcUpd
        Mockito.verify(this.mockedSession, Mockito.times(3)).sendMessage(Mockito.any(Message.class));
        assertEquals(3, this.sendMessages.size());
        assertTrue(this.sendMessages.get(2) instanceof Pcrpt);
    }

    @Test
    public void testSessionListenerPcErr() throws UnknownHostException {
        final SimpleSessionListener sessionListser = new SimpleSessionListener(1, true, 1,
                InetAddress.getByName("127.0.0.1"));

        sessionListser.onMessage(this.mockedSession, createUpdMsg());
        // send PcErr as a response to PcUpd
        Mockito.verify(this.mockedSession, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
        assertEquals(1, this.sendMessages.size());
        assertTrue(this.sendMessages.get(0) instanceof Pcerr);
    }

    private Pcupd createUpdMsg() {
        final PcupdMessageBuilder msgBuilder = new PcupdMessageBuilder();
        final UpdatesBuilder updsBuilder = new UpdatesBuilder();
        updsBuilder.setLsp(new LspBuilder().setPlspId(new PlspId(1L)).build());
        final PathBuilder pathBuilder = new PathBuilder();
        pathBuilder.setEro(new EroBuilder().build());
        updsBuilder.setPath(pathBuilder.build());
        updsBuilder.setSrp(new SrpBuilder().setOperationId(new SrpIdNumber(0L)).build());
        msgBuilder.setUpdates(Lists.newArrayList(updsBuilder.build()));
        return new PcupdBuilder().setPcupdMessage(msgBuilder.build()).build();
    }
}
