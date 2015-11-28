/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

public class PCCSyncOptimizationTest extends PCCMockCommon {

    private final BigInteger initialDBVersionSync = BigInteger.valueOf(1);
    private final BigInteger initialDBVersionDesync = BigInteger.valueOf(1);
    private final String[] mainInputSyncAvoidanceSynchronized = new String[]{"--local-address", PCCMockTest.LOCAL_ADDRESS, "--remote-address",
        PCCMockTest.REMOTE_ADDRESS + ":4567", "--pcc", "1", "--lsp", "3", "--log-level", "DEBUG", "-ka", "10", "-d", "40", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1", "--state-sync-avoidance", "1", "-1", "-1"};

    private final String[] mainInputSyncAvoidanceDesynchronized = new String[]{"--local-address", PCCMockTest.LOCAL_ADDRESS, "--remote-address",
        PCCMockTest.REMOTE_ADDRESS + ":4567", "--pcc", "1", "--lsp", "3", "--log-level", "DEBUG", "-ka", "10", "-d", "40", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1", "--state-sync-avoidance", "10", "5", "5"};

    @Test
    public void testSessionAvoidanceSynchronizedEstablishment() throws UnknownHostException, InterruptedException, ExecutionException {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, socket, new PCCPeerProposal(initialDBVersionSync));
        Main.main(mainInputSyncAvoidanceSynchronized);
        Thread.sleep(1000);
        final TestingSessionListener pceSessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(PCCMockTest.LOCAL_ADDRESS));
        checkSynchronizedSession(pceSessionListener);
        channel.close().get();
        Thread.sleep(1000);
    }

    @Test
    public void testSessionAvoidanceDesynchronizedEstablishment() throws UnknownHostException, InterruptedException, ExecutionException {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, socket, new PCCPeerProposal(initialDBVersionDesync));
        Main.main(mainInputSyncAvoidanceDesynchronized);
        Thread.sleep(2000);
        final TestingSessionListener pceSessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(PCCMockTest.LOCAL_ADDRESS));
        checkSynchronizedSession(pceSessionListener);
        Thread.sleep(4000);
        assertFalse(pceSessionListener.isUp());
        Thread.sleep(6000);
        Thread.sleep(1000);
        checkResyncSession(factory.getSessionListenerByRemoteAddress(InetAddresses.forString(PCCMockTest.LOCAL_ADDRESS)));
        channel.close().get();
        Thread.sleep(1000);
    }

    private void checkResyncSession(final TestingSessionListener pceSessionListener) {
        assertTrue(pceSessionListener.isUp());
        //Send Open with LspDBV = 10
        final List<Message> messages = pceSessionListener.messages();
        final Long expectedDBVersion = Long.valueOf(10);
        checkSequequenceDBVersionSync(messages, true, expectedDBVersion);
        assertEquals(4, messages.size());
        final PCEPSession session = pceSessionListener.getSession();

        checkSession(session, 40, 10);

        assertTrue(session.getRemoteTlvs().getAugmentation(Tlvs1.class).getStateful().getAugmentation(Stateful1.class).isInitiation());
        final BigInteger pceDBVersion = session.localSessionCharacteristics().getAugmentation(Tlvs3.class).getLspDbVersion().getLspDbVersionValue();
        assertEquals(BigInteger.ONE, pceDBVersion);

        final BigInteger pccDBVersion = session.getRemoteTlvs().getAugmentation(Tlvs3.class).getLspDbVersion().getLspDbVersionValue();
        assertEquals(BigInteger.valueOf(expectedDBVersion), pccDBVersion);
    }

    private void checkSynchronizedSession(final TestingSessionListener pceSessionListener) throws InterruptedException {
        assertTrue(pceSessionListener.isUp());
        Thread.sleep(1000);
        //Send Open with LspDBV = 1
        final List<Message> messages = pceSessionListener.messages();
        checkSequequenceDBVersionSync(messages, false, Long.valueOf(1));
        assertEquals(3, messages.size());
        final PCEPSession session = pceSessionListener.getSession();
        checkSession(session, 40, 10);

        assertTrue(session.getRemoteTlvs().getAugmentation(Tlvs1.class).getStateful().getAugmentation(Stateful1.class).isInitiation());
        assertEquals(BigInteger.ONE, session.localSessionCharacteristics().getAugmentation(Tlvs3.class).getLspDbVersion().getLspDbVersionValue());
    }

    /**
     * Really need it ?
     **/
    @Override
    protected List<PCEPCapability> getCapabilities() {
        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(true, false, false, false, false, false, true));
        return caps;
    }
}
