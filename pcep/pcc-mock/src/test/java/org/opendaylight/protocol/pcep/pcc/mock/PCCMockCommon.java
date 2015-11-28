/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

public abstract class PCCMockCommon {
    protected static final String REMOTE_ADDRESS = "127.0.1.0";
    protected static final String LOCAL_ADDRESS = "127.0.0.1";
    private final static short KEEP_ALIVE = 40;
    private final static short DEAD_TIMER = 120;
    protected final InetSocketAddress socket = new InetSocketAddress(PCCMockCommon.REMOTE_ADDRESS, getPort());
    private PCEPDispatcher pceDispatcher;
    private PCCDispatcherImpl pccDispatcher;
    protected PCCSessionListener pccSessionListener;

    protected abstract List<PCEPCapability> getCapabilities();

    protected abstract int getPort();

    @Before
    public void setUp() {
        final BasePCEPSessionProposalFactory proposal = new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE, getCapabilities());
        final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(proposal, 0);
        this.pceDispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
            nf, new NioEventLoopGroup(), new NioEventLoopGroup());
    }

    protected static TestingSessionListener checkSessionListener(final int numMessages, final Channel channel, final TestingSessionListenerFactory factory, final String localAddress) throws
        ExecutionException, InterruptedException {
        final TestingSessionListener sessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(localAddress));
        assertNotNull(sessionListener);
        assertTrue(sessionListener.isUp());
        assertEquals(numMessages, sessionListener.messages().size());
        channel.close().get();
        return sessionListener;
    }

    protected Channel createServer(final TestingSessionListenerFactory factory, final InetSocketAddress serverAddress2) {
        return createServer(factory, serverAddress2, null);
    }

    protected Channel createServer(final TestingSessionListenerFactory factory, final InetSocketAddress
        serverAddress2, final PCEPPeerProposal peerProposal) {
        final PCEPExtensionProviderContext ctx = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance();
        final StatefulActivator activator07 = new StatefulActivator();
        final SyncOptimizationsActivator optimizationsActivator = new SyncOptimizationsActivator();
        activator07.start(ctx);
        optimizationsActivator.start(ctx);
        return this.pceDispatcher.createServer(serverAddress2, factory, peerProposal).channel();
    }

    protected static void checkSynchronizedSession(final int numberOfLsp, final TestingSessionListener pceSessionListener) throws
        InterruptedException {
        assertTrue(pceSessionListener.isUp());
        Thread.sleep(1000);
        //Send Open with LspDBV = 1
        final List<Message> messages = pceSessionListener.messages();
        checkSequequenceDBVersionSync(messages, false, Long.valueOf(1));
        assertEquals(numberOfLsp, messages.size());
        final PCEPSession session = pceSessionListener.getSession();
        checkSession(session, DEAD_TIMER, KEEP_ALIVE);

        assertTrue(session.getRemoteTlvs().getAugmentation(Tlvs1.class).getStateful().getAugmentation(Stateful1.class).isInitiation());
        assertEquals(BigInteger.ONE, session.localSessionCharacteristics().getAugmentation(Tlvs3.class).getLspDbVersion().getLspDbVersionValue());
    }

    protected static void checkResyncSession(final int expetedNumberOfLsp, final Long expectedDBVersion, final TestingSessionListener
        pceSessionListener) {
        assertTrue(pceSessionListener.isUp());
        //Send Open with LspDBV = 10
        final List<Message> messages = pceSessionListener.messages();
        checkSequequenceDBVersionSync(messages, true, expectedDBVersion);
        assertEquals(expetedNumberOfLsp, messages.size());
        final PCEPSession session = pceSessionListener.getSession();

        checkSession(session, DEAD_TIMER, KEEP_ALIVE);

        assertTrue(session.getRemoteTlvs().getAugmentation(Tlvs1.class).getStateful().getAugmentation(Stateful1.class).isInitiation());
        final BigInteger pceDBVersion = session.localSessionCharacteristics().getAugmentation(Tlvs3.class).getLspDbVersion().getLspDbVersionValue();
        assertEquals(BigInteger.ONE, pceDBVersion);

        final BigInteger pccDBVersion = session.getRemoteTlvs().getAugmentation(Tlvs3.class).getLspDbVersion().getLspDbVersionValue();
        assertEquals(BigInteger.valueOf(expectedDBVersion), pccDBVersion);
    }

    protected static void checkSession(final PCEPSession session, final int expectedDeadTimer, final int expectedKeepAlive) {
        assertNotNull(session);
        assertEquals(expectedDeadTimer, session.getPeerPref().getDeadtimer().shortValue());
        assertEquals(expectedKeepAlive, session.getPeerPref().getKeepalive().shortValue());
        final Stateful1 stateful = session.getRemoteTlvs().getAugmentation(Tlvs1.class).getStateful().getAugmentation(Stateful1.class);
        assertTrue(stateful.isInitiation());
    }

    protected static void checkSequequenceDBVersionSync(final List<Message> messages, final boolean isSyncRequired, final Long expectedDBVersion) {
        BigInteger expectedDbVersion = BigInteger.valueOf(expectedDBVersion);
        for (Message msg : messages) {
            final List<Reports> pcrt = ((Pcrpt) msg).getPcrptMessage().getReports();
            for (Reports report : pcrt) {
                if (!isSyncRequired) {
                    expectedDbVersion = expectedDbVersion.add(BigInteger.ONE);
                }
                final Lsp lsp = report.getLsp();
                if (lsp.getPlspId().getValue() == 0) {
                    assertEquals(!isSyncRequired, lsp.isSync().booleanValue());
                } else {
                    assertEquals(isSyncRequired, lsp.isSync().booleanValue());
                }
                final BigInteger actuaLspDBVersion = lsp.getTlvs().getAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params
                    .xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1.class).getLspDbVersion().getLspDbVersionValue();
                assertEquals(expectedDbVersion, actuaLspDBVersion);
            }
        }
    }

    protected Future<PCEPSession> createPCCSession(BigInteger DBVersion) {
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
            }, snf, null, getLocalAdress(), DBVersion);
    }

    private InetSocketAddress getLocalAdress() {
        return new InetSocketAddress(PCCMockTest.LOCAL_ADDRESS, getPort());
    }

    private InetSocketAddress getRemoteAdress() {
        return new InetSocketAddress(PCCMockTest.REMOTE_ADDRESS, getPort());
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE, getCapabilities()), 0);
    }
}
