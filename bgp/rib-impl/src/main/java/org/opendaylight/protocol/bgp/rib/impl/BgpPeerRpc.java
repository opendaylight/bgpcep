/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelFuture;
import java.util.Set;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.PeerRPCs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSessionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.ResetSessionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefullyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefullyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RestartGracefullyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.RouteRefreshRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BgpPeerRpc {
    private static final Logger LOG = LoggerFactory.getLogger(BgpPeerRpc.class);

    private final BGPSession session;
    private final Set<TablesKey> supportedFamilies;
    private final PeerRPCs peerRPCs;

    BgpPeerRpc(final PeerRPCs peerRPCs, final BGPSession session, final Set<TablesKey> supportedFamilies) {
        this.session = requireNonNull(session);
        this.peerRPCs = requireNonNull(peerRPCs);
        this.supportedFamilies = requireNonNull(supportedFamilies);
    }

    ListenableFuture<RpcResult<ResetSessionOutput>> resetSession(final ResetSessionInput input) {
        final var f = peerRPCs.releaseConnection();
        return Futures.transform(f,
            input1 -> f.isDone() ? RpcResultBuilder.success(new ResetSessionOutputBuilder().build()).build()
                : RpcResultBuilder.<ResetSessionOutput>failed()
                    .withError(ErrorType.RPC, "Failed to reset session")
                    .build(),
            MoreExecutors.directExecutor());
    }

    ListenableFuture<RpcResult<RestartGracefullyOutput>> restartGracefully(final RestartGracefullyInput input) {
        final var ret = SettableFuture.<RpcResult<RestartGracefullyOutput>>create();
        Futures.addCallback(peerRPCs.restartGracefully(input.getSelectionDeferralTime().toJava()),
            new FutureCallback<Object>() {
                @Override
                public void onSuccess(final Object result) {
                    ret.set(RpcResultBuilder.success(new RestartGracefullyOutputBuilder().build()).build());
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.error("Failed to perform graceful restart", throwable);
                    ret.set(RpcResultBuilder.<RestartGracefullyOutput>failed()
                        .withError(ErrorType.RPC, throwable.getMessage()).build());
                }
            }, MoreExecutors.directExecutor());
        return ret;
    }

    ListenableFuture<RpcResult<RouteRefreshRequestOutput>> routeRefreshRequest(final RouteRefreshRequestInput input) {
        final var f = sendRRMessage(input);
        if (f == null) {
            return RpcResultBuilder.<RouteRefreshRequestOutput>failed().withError(ErrorType.RPC,
                "Failed to send Route Refresh message due to unsupported address families.").buildFuture();
        }

        final var ret = SettableFuture.<RpcResult<RouteRefreshRequestOutput>>create();
        f.addListener(future -> ret.set(future.isSuccess()
            ? RpcResultBuilder.success(new RouteRefreshRequestOutputBuilder().build()).build()
                : RpcResultBuilder.<RouteRefreshRequestOutput>failed()
                    .withError(ErrorType.RPC, "Failed to send Route Refresh message")
                    .build())
        );
        return ret;
    }

    private ChannelFuture sendRRMessage(final RouteRefreshRequestInput input) {
        if (!supportedFamilies.contains(new TablesKey(input.getAfi(), input.getSafi()))) {
            LOG.info("Unsupported afi/safi: {}, {}.", input.getAfi(), input.getSafi());
            return null;
        }
        return ((BGPSessionImpl) session).getLimiter()
            .writeAndFlush(new RouteRefreshBuilder().setAfi(input.getAfi()).setSafi(input.getSafi()).build());
    }
}
