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
import io.netty.channel.ChannelFuture;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev160322.PeerRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev160322.RouteRefreshRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev160322.RouteRefreshRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class BgpPeerRpcTest {

    private final Set<TablesKey> supportedFamilies = new HashSet<>();
    private final BGPSessionImpl session = Mockito.mock(BGPSessionImpl.class);
    private final BgpPeerRpc rpc = new BgpPeerRpc(this.session, this.supportedFamilies);
    private final PeerRef peer = Mockito.mock(PeerRef.class);

    private final ChannelOutputLimiter limiter = new ChannelOutputLimiter(this.session);
    private final ChannelFuture future = Mockito.mock(ChannelFuture.class);

    @Before
    public void setUp() throws InterruptedException, ExecutionException {
        this.supportedFamilies.add(new TablesKey(Ipv4AddressFamily.class, SubsequentAddressFamily.class));

        Mockito.doReturn(this.limiter).when(this.session).getLimiter();
        Mockito.doReturn(this.future).when(this.session).writeAndFlush(Mockito.any(Notification.class));
        Mockito.doReturn(true).when(this.future).isDone();
        Mockito.doReturn(null).when(this.future).get();
        Mockito.doReturn(true).when(this.future).isSuccess();
    }

    @Test
    public void testSuccessRequest() throws InterruptedException, ExecutionException {
        final RouteRefreshRequestInput input = new RouteRefreshRequestInputBuilder()
            .setAfi(Ipv4AddressFamily.class)
            .setSafi(SubsequentAddressFamily.class)
            .setPeerRef(this.peer).build();
        final Future<RpcResult<Void>> result = this.rpc.routeRefreshRequest(input);
        assertTrue(result.get().getErrors().isEmpty());
    }

    @Test
    public void testFailedRequest() throws InterruptedException, ExecutionException {
        final RouteRefreshRequestInput input = new RouteRefreshRequestInputBuilder()
            .setAfi(Ipv6AddressFamily.class)
            .setSafi(SubsequentAddressFamily.class)
            .setPeerRef(this.peer).build();
        final Future<RpcResult<Void>> result = this.rpc.routeRefreshRequest(input);
        assertEquals(1, result.get().getErrors().size());
        assertEquals("Failed to send Route Refresh message due to unsupported address families.", result.get().getErrors().iterator().next().getMessage());
    }
}
