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

import com.google.common.util.concurrent.Futures;
import io.netty.channel.ChannelFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.rib.spi.PeerRPCs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev171027.PeerRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev171027.ReleaseConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev171027.ReleaseConnectionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev171027.RouteRefreshRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev171027.RouteRefreshRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.RpcResult;

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
        MockitoAnnotations.initMocks(this);
        this.rpc = new BgpPeerRpc(this.peerRpcs, this.session,
                Collections.singleton(new TablesKey(Ipv4AddressFamily.class, SubsequentAddressFamily.class)));
        final ChannelOutputLimiter limiter = new ChannelOutputLimiter(this.session);

        Mockito.doReturn(limiter).when(this.session).getLimiter();
        Mockito.doReturn(this.future).when(this.session).writeAndFlush(Mockito.any(Notification.class));
        Mockito.doReturn(true).when(this.future).isDone();
        Mockito.doReturn(null).when(this.future).get();
        Mockito.doReturn(true).when(this.future).isSuccess();
    }

    @Test
    public void testRouteRefreshRequestSuccessRequest() throws InterruptedException, ExecutionException {
        final RouteRefreshRequestInput input = new RouteRefreshRequestInputBuilder()
                .setAfi(Ipv4AddressFamily.class)
                .setSafi(SubsequentAddressFamily.class)
                .setPeerRef(this.peer).build();
        final Future<RpcResult<Void>> result = this.rpc.routeRefreshRequest(input);
        assertTrue(result.get().getErrors().isEmpty());
    }

    @Test
    public void testRouteRefreshRequestFailedRequest() throws InterruptedException, ExecutionException {
        final RouteRefreshRequestInput input = new RouteRefreshRequestInputBuilder()
                .setAfi(Ipv6AddressFamily.class)
                .setSafi(SubsequentAddressFamily.class)
                .setPeerRef(this.peer).build();
        final Future<RpcResult<Void>> result = this.rpc.routeRefreshRequest(input);
        assertEquals(1, result.get().getErrors().size());
        assertEquals("Failed to send Route Refresh message due to unsupported address families.",
                result.get().getErrors().iterator().next().getMessage());
    }

    @Test
    public void testResetSessionRequestSuccessRequest() throws InterruptedException, ExecutionException {
        Mockito.doReturn(Futures.immediateFuture(null)).when(this.peerRpcs).releaseConnection();
        final ReleaseConnectionInput input = new ReleaseConnectionInputBuilder()
                .setPeerRef(this.peer).build();
        final Future<RpcResult<Void>> result = this.rpc.releaseConnection(input);
        assertTrue(result.get().getErrors().isEmpty());
    }
}
