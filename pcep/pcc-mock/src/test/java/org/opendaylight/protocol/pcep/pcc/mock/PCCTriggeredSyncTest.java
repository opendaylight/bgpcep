/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertNotNull;

import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

public class PCCTriggeredSyncTest extends PCCMockCommon {

    private Channel channel;
    private PCCSessionListener pccSessionListener;
    private PCCDispatcherImpl pccDispatcher;

    private Future<PCEPSession> createPCCSession() {
        this.pccDispatcher = new PCCDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry());
        final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf = getSessionNegotiatorFactory();
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(3, getLocalAdress().getAddress(), 0, -1, new HashedWheelTimer());

        return pccDispatcher.createClient(getRemoteAdress(), -1,
            new PCEPSessionListenerFactory() {
                @Override
                public PCEPSessionListener getSessionListener() {
                    pccSessionListener = new PCCSessionListener(1, tunnelManager, false);
                    return pccSessionListener;
                }
            }, snf, null, getLocalAdress(), BigInteger.ONE);
    }

    private InetSocketAddress getLocalAdress() {
        return new InetSocketAddress(PCCMockTest.LOCAL_ADDRESS, getPort());
    }

    private InetSocketAddress getRemoteAdress() {
        return new InetSocketAddress(PCCMockTest.REMOTE_ADDRESS, getPort());
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(40, 10, getCapabilities()), 0);
    }

    @Test
    public void testSessionTriggeredSync() throws UnknownHostException, InterruptedException, ExecutionException {
        Thread.sleep(4000);
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        this.channel = createServer(factory, socket, new PCCPeerProposal());
        Thread.sleep(4000);
        PCEPSession session = null;
        try {
            session = createPCCSession().get();
        } catch (Exception e) {
            // Throw error
        }
        final TestingSessionListener pceSessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(PCCMockTest.LOCAL_ADDRESS));
        assertNotNull(pceSessionListener);
        checkSynchronizedSession(0, pceSessionListener);
        pccSessionListener.onMessage(session, createTriggerMsg());
        Thread.sleep(4000);
        checkSynchronizedSession(3, pceSessionListener);
        this.channel.close().get();
        try {
            session.close();
            this.pccDispatcher.close();
        } catch (Exception e) {
            //Throw error
        }
    }

    private Message createTriggerMsg() {
        final UpdatesBuilder rb = new UpdatesBuilder();
        // create PCUpd with mandatory objects and LSP object set to 1
        final SrpBuilder srpBuilder = new SrpBuilder();
        srpBuilder.setIgnore(false);
        srpBuilder.setProcessingRule(false);
        srpBuilder.setOperationId(new SrpIdNumber(1L));
        final Srp srp = srpBuilder.build();
        rb.setSrp(srp);

        final Lsp lsp = new LspBuilder().setPlspId(new PlspId(Long.valueOf(0))).setSync(Boolean.TRUE).build();
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
        caps.add(new PCEPStatefulCapability(true, true, true, true, false, false, true));
        return caps;
    }

    @Override
    protected int getPort() {
        return 4582;
    }
}

