/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.parser.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.entry.RibOutEntryFactory;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

/**
 * Tests for the BGPCEP-1080 deadlock fix in {@link AbstractPeer#releaseRibOutChain(boolean)}.
 *
 * <p>BGPCEP-1080 was a deadlock between session teardown and RIB-out transaction chain failure. Releasing the
 * RIB-out chain on teardown held the peer's monitor while blocking on the last submitted RIB-out transaction's
 * future via {@code Future#get()}. If that same transaction chain then failed, its failure callback
 * ({@code onRibOutChainFailed}) needed that very same monitor to report the failure and let the transaction
 * settle -- which it could never acquire, so neither side ever made progress.
 *
 * <p>The fix replaced the blocking wait with a non-blocking completion listener, so the monitor is released
 * immediately instead of being held for the duration of the wait. Rather than reproducing the original race with
 * threads and timeouts, these tests verify that mechanism directly: they mock the submitted transaction's future
 * and assert that {@code releaseRibOutChain} never calls the blocking {@code get()}, registers a listener instead,
 * detaches the chain from the peer synchronously so no further writes can be issued against it, and only closes
 * the chain once that listener fires.
 */
public class Bgpcep1080Test extends AbstractRIBTestSetup {
    private static final Ipv4AddressNoZone PEER_ADDRESS = new Ipv4AddressNoZone("127.0.0.1");
    private static final TablesKey IPV4_UNICAST =
        new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);

    @Mock
    private FluentFuture<CommitInfo> submitted;
    @Mock
    private RouteEntryDependenciesContainer entryDep;
    @Captor
    private ArgumentCaptor<FutureCallback<CommitInfo>> listener;

    private BGPPeer peer;
    private DOMTransactionChain ribOutChain;
    private DOMDataTreeWriteTransaction ribOutTx;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        peer = AbstractAddPathTest.configurePeer(tableRegistry, PEER_ADDRESS, getRib(), null, PeerRole.Ibgp,
            new StrictBGPPeerRegistry());

        // Minimal session double, just enough for onSessionUp() to populate peerId/peerPath/peerRibOutIId --
        // no Netty channel/pipeline/event loop needed since we never touch the session afterward.
        final var session = mock(BGPSession.class);
        doReturn(Set.<BgpTableType>of()).when(session).getAdvertisedTableTypes();
        doReturn(new Ipv4Address("1.1.1.1")).when(session).getBgpId();
        doReturn(List.<AddressFamilies>of()).when(session).getAdvertisedAddPathTableTypes();
        doReturn(GracefulRestartUtil.EMPTY_GR_CAPABILITY).when(session).getAdvertisedGracefulRestartCapability();
        doReturn(GracefulRestartUtil.EMPTY_LLGR_CAPABILITY).when(session).getAdvertisedLlGracefulRestartCapability();
        peer.onSessionUp(session);

        // Replace whatever chain onSessionUp() installed with one we fully control.
        ribOutChain = mock(DOMTransactionChain.class);
        ribOutTx = mock(DOMDataTreeWriteTransaction.class);
        doReturn(ribOutTx).when(ribOutChain).newWriteOnlyTransaction();
        doNothing().when(ribOutChain).close();
        peer.ribOutChain = ribOutChain;
    }

    /**
     * Releasing a pending RIB-out chain must not block on the submitted transaction's future.
     *
     * <p>With a not-yet-completed submitted transaction, {@code releaseRibOutChain(true)} must not call the blocking
     * {@link java.util.concurrent.Future#get()} -- that is exactly what caused the original deadlock, since it ran
     * while holding the peer's monitor. It must instead register a completion listener and leave the chain open
     * until that listener fires.
     */
    @Test
    public void releaseRibOutChainDoesNotBlockOnPendingSubmit() throws Exception {
        doReturn(false).when(submitted).isDone();
        arm();

        peer.releaseRibOutChain(true);

        verify(submitted, never()).get();
        verify(ribOutChain, never()).close();
        // The callback is added twice one on commit and one on releaseRibOutChain
        verify(submitted, times(2)).addCallback(listener.capture(), eq(MoreExecutors.directExecutor()));

        // Verify the peer is not blocked and releaseRibOutChain can be executed again without blocking and
        // without any effect.
        peer.releaseRibOutChain(false);
        verify(ribOutChain, never()).close();

        // Simulate the submitted transaction settling: the chain must close only now, not before.
        listener.getValue().onSuccess(null);
        verify(ribOutChain).close();
    }

    /**
     * Releasing the RIB-out chain closes it immediately once the submitted transaction is already done.
     *
     * <p>When the submitted transaction has already completed, {@code releaseRibOutChain(true)} has nothing to wait
     * for: it must close the chain synchronously, without registering a listener.
     */
    @Test
    public void releaseRibOutChainClosesImmediatelyWhenSubmitDone() throws Exception {
        doReturn(true).when(submitted).isDone();
        arm();

        peer.releaseRibOutChain(true);

        verify(submitted, never()).get();
        verify(submitted, never()).addListener(any(Runnable.class), any(Executor.class));
        verify(ribOutChain).close();
    }

    /**
     * Releasing the RIB-out chain without waiting ignores the submitted transaction entirely.
     *
     * <p>{@code releaseRibOutChain(false)} is used on the chain-failure teardown path, which must not wait for the
     * submitted transaction at all: the chain is closed immediately regardless of whether that transaction is
     * done, and neither {@code get()} nor a listener is ever touched on it.
     */
    @Test
    public void releaseRibOutChainIgnoresSubmitWhenNotWaiting() throws Exception {
        doReturn(false).when(submitted).isDone();
        arm();

        peer.releaseRibOutChain(false);

        verify(submitted, never()).get();
        verify(submitted, never()).addListener(any(Runnable.class), any(Executor.class));
        verify(ribOutChain).close();
    }

    /**
     * Releasing the RIB-out chain detaches it from the peer synchronously, before any waiting.
     *
     * <p>The chain must be detached from the peer synchronously, before any waiting happens -- not only once the
     * listener eventually closes it. Otherwise a write racing in between {@code releaseRibOutChain(true)} and the
     * submitted transaction settling could still be issued against a chain that is being torn down.
     */
    @Test
    public void releaseRibOutChainDetachesBeforeClosing() {
        doReturn(false).when(submitted).isDone();
        arm();

        peer.releaseRibOutChain(true);

        // The chain is detached synchronously, even though close() is still pending on the listener: further
        // writes must not be issued against it.
        clearInvocations(ribOutChain);
        submitRibOutTransaction();
        verify(ribOutChain, never()).newWriteOnlyTransaction();
    }

    /**
     * Make the peer write to its RIB-out, which is what arms {@code AbstractPeer.submitted}. The route lists are
     * empty: we only care about the transaction being submitted, not about its contents.
     */
    private void submitRibOutTransaction() {
        doReturn(getRib().getRibSupportContext().getRIBSupport(IPV4_UNICAST)).when(entryDep).getRIBSupport();
        doReturn(getRib().getPeerTracker()).when(entryDep).getPeerTracker();
        peer.refreshRibOut(entryDep, List.of(), List.of(), RibOutEntryFactory.unshared());
    }

    private void arm() {
        doNothing().when(submitted).addCallback(any(), any());
        doReturn(submitted).when(ribOutTx).commit();
        submitRibOutTransaction();
    }
}
