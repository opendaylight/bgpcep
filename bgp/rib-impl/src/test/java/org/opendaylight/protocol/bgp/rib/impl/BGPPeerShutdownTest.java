/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint16;

public class BGPPeerShutdownTest extends AbstractRIBTestSetup {
    private static final Ipv4AddressNoZone PEER_ADDRESS = new Ipv4AddressNoZone("127.0.0.1");
    private static final TablesKey IPV4_UNICAST =
        new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    // Time given to the session teardown to either complete or get stuck on the pending transaction
    private static final long SETTLE_SECONDS = 2;
    // Time given to the chain failure to be processed. It does not wait for anything, hence anything above a few
    // seconds means it is blocked on the peer monitor.
    private static final long AWAIT_SECONDS = 5;
    // Upper bound for cleanup, so that a failure never hangs the build
    private static final long CLEANUP_SECONDS = 5;

    @Captor
    private ArgumentCaptor<FutureCallback<Empty>> captor;

    private BGPPeer peer;
    private BGPSessionImpl session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        peer = AbstractAddPathTest.configurePeer(tableRegistry, PEER_ADDRESS, getRib(), null, PeerRole.Ibgp,
            new StrictBGPPeerRegistry());
        session = mockSession(peer);
    }

    /**
     * Test of session teardown racing with a failure of the RIB-out transaction chain.
     *
     * <p>{@link BGPPeer#onSessionDown} holds the peer's monitor while releasing the RIB-out chain.
     * Verify that waiting for last submitted RIB-out transaction to complete do not block RIB-out transaction chain
     * failure, and both threads finish.
     * Potential blocking of transaction chain failure could prevent submit to complete as it's waiting for failure
     * callback before finishing resulting in deadlock.
     */
    @Test
    public void testRibOutChainFailureDuringSessionDown() throws Exception {
        peer.onSessionUp(session);
        verify(peer.ribOutChain, atLeastOnce()).addCallback(captor.capture());
        final var ribOutCallback = captor.getValue();

        // Have the peer submit an RIB-out transaction which does not complete, so that it becomes the transaction
        // session teardown may end up waiting for
        final var pendingCommit = SettableFuture.<CommitInfo>create();
        doReturn(FluentFuture.from(pendingCommit)).when(getTransaction()).commit();
        submitRibOutTransaction();

        // Thread #1: the netty thread reporting the session going down. It acquires the peer's monitor and, if it waits
        // for the pending commit, never releases it.
        final var sessionDown = startThread("session-down",
            () -> peer.onSessionDown(session, new IllegalStateException("simulated session failure")));
        sessionDown.join(TimeUnit.SECONDS.toMillis(SETTLE_SECONDS));

        // Thread #2: the transaction chain reporting failure. It has to be able to complete even though the pending
        // commit never will -- chain failure handling explicitly does not wait for the submitted transaction.
        final var chainFailed = startThread("ribout-chain-failed",
            () -> ribOutCallback.onFailure(new IllegalStateException("simulated chain failure")));

        try {
            chainFailed.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS));
            if (chainFailed.isAlive()) {
                fail("RIB-out chain failure was not processed within " + AWAIT_SECONDS + "s, deadlocked with session "
                    + "teardown:\n" + dumpStack(sessionDown) + dumpStack(chainFailed));
            }
        } finally {
            // Unblock whatever is still waiting, so that neither the test nor the JVM hangs
            pendingCommit.set(CommitInfo.empty());
            sessionDown.join(TimeUnit.SECONDS.toMillis(CLEANUP_SECONDS));
            chainFailed.join(TimeUnit.SECONDS.toMillis(CLEANUP_SECONDS));
        }

        assertFalse("Session teardown did not complete:\n" + dumpStack(sessionDown), sessionDown.isAlive());
    }

    /**
     * Make the peer write to its RIB-out, which is what arms {@code AbstractPeer.submitted}. The route lists are empty:
     * we only care about the transaction being submitted, not about its contents.
     */
    private void submitRibOutTransaction() {
        final var entryDep = mock(RouteEntryDependenciesContainer.class);
        doReturn(getRib().getRibSupportContext().getRIBSupport(IPV4_UNICAST)).when(entryDep).getRIBSupport();
        doReturn(getRib().getPeerTracker()).when(entryDep).getPeerTracker();
        peer.refreshRibOut(entryDep, List.of(), List.of());
    }

    private static Thread startThread(final String name, final Runnable body) {
        final var ret = new Thread(body, name);
        // A deadlocked thread cannot be joined, so make sure it cannot keep the JVM alive either
        ret.setDaemon(true);
        ret.start();
        return ret;
    }

    private static String dumpStack(final Thread thread) {
        final var sb = new StringBuilder().append('"').append(thread.getName()).append("\" ").append(thread.getState())
            .append('\n');
        for (var frame : thread.getStackTrace()) {
            sb.append("\tat ").append(frame).append('\n');
        }
        return sb.toString();
    }

    private static BGPSessionImpl mockSession(final BGPSessionListener listener) {
        final var eventLoop = mock(EventLoop.class);
        final var channel = mock(Channel.class);
        final var pipeline = mock(ChannelPipeline.class);
        doReturn(null).when(eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn(eventLoop).when(channel).eventLoop();
        doReturn(Boolean.TRUE).when(channel).isWritable();
        doReturn(null).when(channel).close();
        doReturn(pipeline).when(channel).pipeline();
        doCallRealMethod().when(channel).toString();
        doReturn(pipeline).when(pipeline).addLast(any(ChannelHandler.class));
        doReturn(new DefaultChannelPromise(channel)).when(channel).writeAndFlush(any(Notification.class));
        doReturn(new InetSocketAddress("localhost", 12345)).when(channel).remoteAddress();
        doReturn(new InetSocketAddress("localhost", 12345)).when(channel).localAddress();

        final var params = List.of(new BgpParametersBuilder()
            .setOptionalCapabilities(List.of(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .addAugmentation(new CParameters1Builder()
                        .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                            .setAfi(Ipv4AddressFamily.VALUE)
                            .setSafi(UnicastSubsequentAddressFamily.VALUE)
                            .build())
                        .build())
                    .build())
                .build()))
            .build());
        final var openObj = new OpenBuilder()
            .setBgpIdentifier(new Ipv4AddressNoZone("1.1.1.1"))
            .setHoldTimer(Uint16.valueOf(50))
            .setMyAsNumber(Uint16.valueOf(72))
            .setBgpParameters(params)
            .build();

        final var ret = new BGPSessionImpl(listener, channel, openObj, 30, null);
        ret.setChannelExtMsgCoder(openObj);
        return ret;
    }
}
