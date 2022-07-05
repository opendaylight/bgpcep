/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.rib.spi.PeerRPCs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.PeerRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSessionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSessionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefullyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefullyInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefullyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public final class BgpPeerRpcTest {
    @Mock
    private BGPSessionImpl session;
    @Mock
    private PeerRPCs peerRpcs;
    @Mock
    private PeerRef peer;
    @Mock
    private ChannelFuture future;
    private BgpPeerRpc rpc;

    @Before
    public void setUp() throws InterruptedException, ExecutionException {
        rpc = new BgpPeerRpc(peerRpcs, session,
                Set.of(new TablesKey(Ipv4AddressFamily.VALUE, SubsequentAddressFamily.VALUE)));
        final ChannelOutputLimiter limiter = new ChannelOutputLimiter(session);

        doReturn(limiter).when(session).getLimiter();
        doReturn(future).when(session).writeAndFlush(any(Notification.class));

        doReturn(true).when(future).isSuccess();
        doAnswer(invocation -> {
            GenericFutureListener<ChannelFuture> listener = invocation.getArgument(0);
            listener.operationComplete(future);
            return null;
        }).when(future).addListener(any());
    }

    @Test
    public void testRouteRefreshRequestSuccessRequest() throws InterruptedException, ExecutionException {
        final RouteRefreshRequestInput input = new RouteRefreshRequestInputBuilder()
                .setAfi(Ipv4AddressFamily.VALUE)
                .setSafi(SubsequentAddressFamily.VALUE)
                .setPeerRef(peer).build();
        final Future<RpcResult<RouteRefreshRequestOutput>> result = rpc.routeRefreshRequest(input);
        assertTrue(result.get().getErrors().isEmpty());
    }

    @Test
    public void testRouteRefreshRequestFailedRequest() throws InterruptedException, ExecutionException {
        final RouteRefreshRequestInput input = new RouteRefreshRequestInputBuilder()
                .setAfi(Ipv6AddressFamily.VALUE)
                .setSafi(SubsequentAddressFamily.VALUE)
                .setPeerRef(peer).build();
        final Future<RpcResult<RouteRefreshRequestOutput>> result = rpc.routeRefreshRequest(input);
        assertEquals(1, result.get().getErrors().size());
        assertEquals("Failed to send Route Refresh message due to unsupported address families.",
                result.get().getErrors().iterator().next().getMessage());
    }

    @Test
    public void testResetSessionRequestSuccessRequest() throws InterruptedException, ExecutionException {
        doReturn(Futures.immediateFuture(null)).when(peerRpcs).releaseConnection();
        final ResetSessionInput input = new ResetSessionInputBuilder()
                .setPeerRef(peer).build();
        final Future<RpcResult<ResetSessionOutput>> result = rpc.resetSession(input);
        assertTrue(result.get().getErrors().isEmpty());
    }

    @Test
    public void testRestartGracefullyRequestFailedRequest() throws ExecutionException, InterruptedException {
        final long referraltimerSeconds = 10L;
        doReturn(new SimpleSessionListener().restartGracefully(referraltimerSeconds))
                .when(peerRpcs).restartGracefully(referraltimerSeconds);
        final RestartGracefullyInput input = new RestartGracefullyInputBuilder()
                .setSelectionDeferralTime(Uint32.valueOf(referraltimerSeconds))
                .build();
        final ListenableFuture<RpcResult<RestartGracefullyOutput>> result = rpc.restartGracefully(input);
        assertTrue(!result.get().getErrors().isEmpty());
    }
}
