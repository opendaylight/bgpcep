/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import com.google.common.util.concurrent.Futures;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.rib.spi.PeerRPCs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.PeerRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSessionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefullyInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint32;

@ExtendWith(MockitoExtension.class)
final class BgpPeerRpcTest {
    @Mock
    private BGPSessionImpl session;
    @Mock
    private PeerRPCs peerRpcs;
    @Mock
    private PeerRef peer;
    @Mock
    private ChannelFuture future;
    private BgpPeerRpc rpc;

    @BeforeEach
    void beforeEach() {
        rpc = new BgpPeerRpc(peerRpcs, session,
            Set.of(new TablesKey(Ipv4AddressFamily.VALUE, SubsequentAddressFamily.VALUE)));
    }

    @Test
    void testRouteRefreshRequestSuccessRequest() throws InterruptedException, ExecutionException {
        final var limiter = new ChannelOutputLimiter(session);

        doReturn(limiter).when(session).getLimiter();
        doReturn(future).when(session).writeAndFlush(any(Notification.class));

        doReturn(true).when(future).isSuccess();
        doAnswer(invocation -> {
            invocation.getArgument(0, ChannelFutureListener.class).operationComplete(future);
            return future;
        }).when(future).addListener(any());

        final var result = rpc.routeRefreshRequest(new RouteRefreshRequestInputBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(SubsequentAddressFamily.VALUE)
            .setPeerRef(peer)
            .build());
        assertTrue(result.get().getErrors().isEmpty());
    }

    @Test
    void testRouteRefreshRequestFailedRequest() throws InterruptedException, ExecutionException {
        final var result = rpc.routeRefreshRequest(new RouteRefreshRequestInputBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(SubsequentAddressFamily.VALUE)
            .setPeerRef(peer)
            .build());
        assertEquals(1, result.get().getErrors().size());
        assertEquals("Failed to send Route Refresh message due to unsupported address families.",
                result.get().getErrors().iterator().next().getMessage());
    }

    @Test
    void testResetSessionRequestSuccessRequest() throws InterruptedException, ExecutionException {
        doReturn(Futures.immediateFuture(null)).when(peerRpcs).releaseConnection();
        final var result = rpc.resetSession(new ResetSessionInputBuilder().setPeerRef(peer).build());
        assertTrue(result.get().getErrors().isEmpty());
    }

    @Test
    void testRestartGracefullyRequestFailedRequest() throws ExecutionException, InterruptedException {
        final long referraltimerSeconds = 10L;
        doReturn(new SimpleSessionListener().restartGracefully(referraltimerSeconds))
                .when(peerRpcs).restartGracefully(referraltimerSeconds);
        final var result = rpc.restartGracefully(new RestartGracefullyInputBuilder()
            .setSelectionDeferralTime(Uint32.valueOf(referraltimerSeconds))
            .build());
        assertTrue(!result.get().getErrors().isEmpty());
    }
}
