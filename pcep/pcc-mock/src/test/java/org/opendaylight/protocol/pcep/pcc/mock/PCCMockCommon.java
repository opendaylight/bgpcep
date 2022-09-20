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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.util.CheckTestUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckTestUtil.checkReceivedMessages;
import static org.opendaylight.protocol.util.CheckTestUtil.waitFutureSuccess;

import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public abstract class PCCMockCommon {
    private static final Uint8 KEEP_ALIVE = Uint8.valueOf(30);
    private static final Uint8 DEAD_TIMER = Uint8.valueOf(120);
    private static final long SLEEP_FOR = 50;
    private final int port = InetSocketAddressUtil.getRandomPort();
    final InetSocketAddress remoteAddress = InetSocketAddressUtil
            .getRandomLoopbackInetSocketAddress(port);
    final InetSocketAddress localAddress = InetSocketAddressUtil
            .getRandomLoopbackInetSocketAddress(port);
    PCCSessionListener pccSessionListener;
    private PCEPDispatcher pceDispatcher;
    private final PCEPExtensionProviderContext extensionProvider = new SimplePCEPExtensionProviderContext();
    private MessageRegistry messageRegistry;

    protected abstract List<PCEPCapability> getCapabilities();

    @Before
    public void setUp() {
        final BasePCEPSessionProposalFactory proposal = new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE,
                getCapabilities());
        final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(proposal, 0);

        ServiceLoader.load(PCEPExtensionProviderActivator.class).forEach(act -> act.start(extensionProvider));

        messageRegistry = extensionProvider.getMessageHandlerRegistry();
        pceDispatcher = new PCEPDispatcherImpl(messageRegistry, nf, new NioEventLoopGroup(),
                new NioEventLoopGroup());
    }

    static TestingSessionListener checkSessionListener(final int numMessages, final Channel channel,
            final TestingSessionListenerFactory factory, final String localAddress) throws Exception {
        final TestingSessionListener sessionListener = checkSessionListenerNotNull(factory, localAddress);
        assertTrue(sessionListener.isUp());
        checkReceivedMessages(sessionListener, numMessages);
        assertEquals(numMessages, sessionListener.messages().size());
        channel.close().get();
        return sessionListener;
    }

    static TestingSessionListener checkSessionListenerNotNull(final TestingSessionListenerFactory factory,
            final String localAddress) {
        final Stopwatch sw = Stopwatch.createStarted();
        TestingSessionListener listener;
        final InetAddress address = InetAddresses.forString(localAddress);
        while (sw.elapsed(TimeUnit.SECONDS) <= 60) {
            listener = factory.getSessionListenerByRemoteAddress(address);
            if (listener == null) {
                Uninterruptibles.sleepUninterruptibly(SLEEP_FOR, TimeUnit.MILLISECONDS);
            } else {
                return listener;
            }
        }
        throw new NullPointerException();
    }

    Channel createServer(final TestingSessionListenerFactory factory,
            final InetSocketAddress serverAddress2) {
        return createServer(factory, serverAddress2, null);
    }

    Channel createServer(final TestingSessionListenerFactory factory, final InetSocketAddress
            serverAddress2, final PCEPPeerProposal peerProposal) {
        final StatefulActivator activator07 = new StatefulActivator();
        final SyncOptimizationsActivator optimizationsActivator = new SyncOptimizationsActivator();
        activator07.start(extensionProvider);
        optimizationsActivator.start(extensionProvider);

        final ChannelFuture future = pceDispatcher
                .createServer(new DispatcherDependencies(serverAddress2, factory, peerProposal));
        waitFutureSuccess(future);
        return future.channel();
    }

    static void checkSynchronizedSession(final int numberOfLsp,
            final TestingSessionListener pceSessionListener, final Uint64 expectedeInitialDb) throws Exception {
        assertTrue(pceSessionListener.isUp());
        //Send Open with LspDBV = 1
        final int numberOfSyncMessage = 1;
        int numberOfLspExpected = numberOfLsp;
        if (!expectedeInitialDb.equals(Uint64.ZERO)) {
            checkEquals(() -> checkSequequenceDBVersionSync(pceSessionListener, expectedeInitialDb));
            numberOfLspExpected += numberOfSyncMessage;
        }
        checkReceivedMessages(pceSessionListener, numberOfLspExpected);
        final PCEPSession session = pceSessionListener.getSession();
        checkSession(session, DEAD_TIMER, KEEP_ALIVE);

        assertTrue(session.getRemoteTlvs().augmentation(Tlvs1.class).getStateful()
                .augmentation(Stateful1.class).getInitiation());
        assertNull(session.getLocalTlvs().augmentation(Tlvs3.class)
                .getLspDbVersion().getLspDbVersionValue());
    }

    static void checkResyncSession(final Optional<Integer> startAtNumberLsp, final int expectedNumberOfLsp,
            final int expectedTotalMessages, final Uint64 startingDBVersion, final Uint64 expectedDBVersion,
            final TestingSessionListener pceSessionListener) throws Exception {
        assertNotNull(pceSessionListener.getSession());
        assertTrue(pceSessionListener.isUp());
        final List<Message> messages;
        checkReceivedMessages(pceSessionListener, expectedTotalMessages);
        if (startAtNumberLsp.isPresent()) {
            messages = pceSessionListener.messages().subList(startAtNumberLsp.get(),
                    startAtNumberLsp.get() + expectedNumberOfLsp);
        } else {
            messages = pceSessionListener.messages();
        }
        checkEquals(() -> checkSequequenceDBVersionSync(pceSessionListener, expectedDBVersion));
        assertEquals(expectedNumberOfLsp, messages.size());
        final PCEPSession session = pceSessionListener.getSession();

        checkSession(session, DEAD_TIMER, KEEP_ALIVE);

        assertTrue(session.getRemoteTlvs().augmentation(Tlvs1.class).getStateful()
                .augmentation(Stateful1.class).getInitiation());
        final Uint64 pceDBVersion = session.getLocalTlvs().augmentation(Tlvs3.class)
                .getLspDbVersion().getLspDbVersionValue();
        assertEquals(startingDBVersion, pceDBVersion);
    }

    static void checkSession(final PCEPSession session, final Uint8 expectedDeadTimer,
            final Uint8 expectedKeepAlive) {
        assertNotNull(session);
        assertEquals(expectedDeadTimer, session.getPeerPref().getDeadtimer());
        assertEquals(expectedKeepAlive, session.getPeerPref().getKeepalive());
        final Stateful1 stateful = session.getRemoteTlvs().augmentation(Tlvs1.class)
                .getStateful().augmentation(Stateful1.class);
        assertTrue(stateful.getInitiation());
    }

    protected static void checkSequequenceDBVersionSync(final TestingSessionListener pceSessionListener,
            final Uint64 expectedDbVersion) {
        for (final Message msg : pceSessionListener.messages()) {
            final List<Reports> pcrt = ((Pcrpt) msg).getPcrptMessage().getReports();
            for (final Reports report : pcrt) {
                final Lsp lsp = report.getLsp();
                if (lsp.getPlspId().getValue().toJava() == 0) {
                    assertEquals(false, lsp.getSync());
                } else {
                    assertEquals(true, lsp.getSync());
                }
                final Uint64 actuaLspDBVersion = lsp.getTlvs()
                        .augmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep
                            .sync.optimizations.rev200720.Tlvs1.class)
                        .getLspDbVersion().getLspDbVersionValue();
                assertEquals(expectedDbVersion, actuaLspDBVersion);
            }
        }
    }

    Future<PCEPSession> createPCCSession(final Uint64 dbVersion) {
        final PCCDispatcherImpl pccDispatcher = new PCCDispatcherImpl(messageRegistry);
        final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf = getSessionNegotiatorFactory();
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(3, localAddress.getAddress(),
                0, -1, new HashedWheelTimer(), Optional.empty());

        return pccDispatcher.createClient(remoteAddress, -1, () -> {
            pccSessionListener = new PCCSessionListener(1, tunnelManager, false);
            return pccSessionListener;
        }, snf, KeyMapping.of(), localAddress, dbVersion);
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE,
                getCapabilities()), 0);
    }

    TestingSessionListener getListener(final TestingSessionListenerFactory factory) {
        return checkSessionListenerNotNull(factory, localAddress.getHostString());
    }

    private static class DispatcherDependencies implements PCEPDispatcherDependencies {
        private final KeyMapping keys = KeyMapping.of();
        private final InetSocketAddress address;
        private final TestingSessionListenerFactory listenerFactory;
        private final PCEPPeerProposal peerProposal;

        DispatcherDependencies(
                final InetSocketAddress address,
                final TestingSessionListenerFactory listenerFactory,
                final PCEPPeerProposal peerProposal) {
            this.address = address;
            this.listenerFactory = listenerFactory;
            this.peerProposal = peerProposal;
        }

        @Override
        public InetSocketAddress getAddress() {
            return address;
        }

        @Override
        public KeyMapping getKeys() {
            return keys;
        }

        @Override
        public PCEPSessionListenerFactory getListenerFactory() {
            return listenerFactory;
        }

        @Override
        public PCEPPeerProposal getPeerProposal() {
            return peerProposal;
        }
    }
}
