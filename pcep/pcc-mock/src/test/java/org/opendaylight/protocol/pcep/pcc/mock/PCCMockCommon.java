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
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckUtil.checkReceivedMessages;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

public abstract class PCCMockCommon {
    private static final short KEEP_ALIVE = 30;
    private static final short DEAD_TIMER = 120;
    private static final long SLEEP_FOR = 50;
    private final int port = InetSocketAddressUtil.getRandomPort();
    protected final InetSocketAddress remoteAddress = InetSocketAddressUtil
        .getRandomLoopbackInetSocketAddress(this.port);
    protected final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", this.port);
    protected PCCSessionListener pccSessionListener;
    private PCEPDispatcher pceDispatcher;
    private PCEPExtensionProviderContext extensionProvider;
    private MessageRegistry messageRegistry;

    protected abstract List<PCEPCapability> getCapabilities();

    @Before
    public void setUp() {
        final BasePCEPSessionProposalFactory proposal = new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE,
            getCapabilities());
        final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(proposal, 0);
        this.extensionProvider  = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance();
        this.messageRegistry = this.extensionProvider.getMessageHandlerRegistry();
        this.pceDispatcher = new PCEPDispatcherImpl(this.messageRegistry, nf, new NioEventLoopGroup(),
            new NioEventLoopGroup());
    }

    protected static TestingSessionListener checkSessionListener(final int numMessages, final Channel channel,
        final TestingSessionListenerFactory factory,
        final String localAddress) throws
        Exception {
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

    protected Channel createServer(final TestingSessionListenerFactory factory,
        final InetSocketAddress serverAddress2) throws InterruptedException {
        return createServer(factory, serverAddress2, null);
    }

    protected Channel createServer(final TestingSessionListenerFactory factory, final InetSocketAddress
        serverAddress2, final PCEPPeerProposal peerProposal) {
        final StatefulActivator activator07 = new StatefulActivator();
        final SyncOptimizationsActivator optimizationsActivator = new SyncOptimizationsActivator();
        activator07.start(this.extensionProvider);
        optimizationsActivator.start(this.extensionProvider);
        final ChannelFuture future = this.pceDispatcher.createServer(serverAddress2, factory, peerProposal);
        waitFutureSuccess(future);
        return future.channel();
    }

    protected static void checkSynchronizedSession(final int numberOfLsp,
        final TestingSessionListener pceSessionListener, final BigInteger expectedeInitialDb) throws Exception {
        assertTrue(pceSessionListener.isUp());
        //Send Open with LspDBV = 1
        final int numberOfSyncMessage = 1;
        int numberOfLspExpected = numberOfLsp;
        if (!expectedeInitialDb.equals(BigInteger.ZERO)) {
            checkEquals(()-> checkSequequenceDBVersionSync(pceSessionListener, expectedeInitialDb));
            numberOfLspExpected += numberOfSyncMessage;
        }
        checkReceivedMessages(pceSessionListener, numberOfLspExpected);
        final PCEPSession session = pceSessionListener.getSession();
        checkSession(session, DEAD_TIMER, KEEP_ALIVE);

        assertTrue(session.getRemoteTlvs().getAugmentation(Tlvs1.class).getStateful()
            .getAugmentation(Stateful1.class).isInitiation());
        assertNull(session.localSessionCharacteristics().getAugmentation(Tlvs3.class)
            .getLspDbVersion().getLspDbVersionValue());
    }

    protected static void checkResyncSession(final Optional<Integer> startAtNumberLsp, final int expectedNumberOfLsp,
        final int expectedTotalMessages, final BigInteger startingDBVersion, final BigInteger expectedDBVersion,
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
        checkEquals(()-> checkSequequenceDBVersionSync(pceSessionListener, expectedDBVersion));
        assertEquals(expectedNumberOfLsp, messages.size());
        final PCEPSession session = pceSessionListener.getSession();

        checkSession(session, DEAD_TIMER, KEEP_ALIVE);

        assertTrue(session.getRemoteTlvs().getAugmentation(Tlvs1.class).getStateful()
            .getAugmentation(Stateful1.class).isInitiation());
        final BigInteger pceDBVersion = session.localSessionCharacteristics().getAugmentation(Tlvs3.class)
            .getLspDbVersion().getLspDbVersionValue();
        assertEquals(startingDBVersion, pceDBVersion);
    }

    protected static void checkSession(final PCEPSession session, final int expectedDeadTimer,
        final int expectedKeepAlive) {
        assertNotNull(session);
        assertEquals(expectedDeadTimer, session.getPeerPref().getDeadtimer().shortValue());
        assertEquals(expectedKeepAlive, session.getPeerPref().getKeepalive().shortValue());
        final Stateful1 stateful = session.getRemoteTlvs().getAugmentation(Tlvs1.class)
            .getStateful().getAugmentation(Stateful1.class);
        assertTrue(stateful.isInitiation());
    }

    protected static void checkSequequenceDBVersionSync(final TestingSessionListener pceSessionListener,
        final BigInteger expectedDbVersion) {
        for (final Message msg : pceSessionListener.messages()) {
            final List<Reports> pcrt = ((Pcrpt) msg).getPcrptMessage().getReports();
            for (final Reports report : pcrt) {
                final Lsp lsp = report.getLsp();
                if (lsp.getPlspId().getValue() == 0) {
                    assertEquals(false, lsp.isSync().booleanValue());
                } else {
                    assertEquals(true, lsp.isSync().booleanValue());
                }
                final BigInteger actuaLspDBVersion = lsp.getTlvs().getAugmentation(org.opendaylight.yang.gen
                    .v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1.class)
                    .getLspDbVersion().getLspDbVersionValue();
                assertEquals(expectedDbVersion, actuaLspDBVersion);
            }
        }
    }

    protected Future<PCEPSession> createPCCSession(final BigInteger dbVersion) {
        final PCCDispatcherImpl pccDispatcher = new PCCDispatcherImpl(this.messageRegistry);
        final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf = getSessionNegotiatorFactory();
        final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(3, this.localAddress.getAddress(),
            0, -1, new HashedWheelTimer(), Optional.absent());

        return pccDispatcher.createClient(this.remoteAddress, -1,
            () -> {
                this.pccSessionListener = new PCCSessionListener(1, tunnelManager, false);
                return this.pccSessionListener;
            }, snf, null, this.localAddress, dbVersion);
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(DEAD_TIMER, KEEP_ALIVE,
            getCapabilities()), 0);
    }

    protected TestingSessionListener getListener(final TestingSessionListenerFactory factory) {
        return checkSessionListenerNotNull(factory, this.localAddress.getHostString());
    }
}
