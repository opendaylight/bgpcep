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
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.ChannelFuture;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.PeerRPCs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329.BgpPeerRpcService;
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
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpPeerRpc implements BgpPeerRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(BgpPeerRpc.class);
    private static final String FAILURE_MSG = "Failed to send Route Refresh message";
    private static final String FAILURE_RESET_SESSION_MSG = "Failed to reset session";
    private static final String FAILURE_GRACEFUL_RESTART_MSG = "Failed to perform graceful restart";

    private final BGPSession session;
    private final Set<TablesKey> supportedFamilies;
    private final PeerRPCs peerRPCs;

    BgpPeerRpc(final PeerRPCs peerRPCs, final BGPSession session, final Set<TablesKey> supportedFamilies) {
        this.session = requireNonNull(session);
        this.peerRPCs = requireNonNull(peerRPCs);
        this.supportedFamilies = requireNonNull(supportedFamilies);
    }

    @Override
    public ListenableFuture<RpcResult<ResetSessionOutput>> resetSession(final ResetSessionInput input) {
        final ListenableFuture<?> f = this.peerRPCs.releaseConnection();
        return Futures.transform(f, input1 -> {
            if (f.isDone()) {
                return RpcResultBuilder.success(new ResetSessionOutputBuilder().build()).build();
            }
            return RpcResultBuilder.<ResetSessionOutput>failed().withError(ErrorType.RPC, FAILURE_RESET_SESSION_MSG)
                    .build();
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<RestartGracefullyOutput>> restartGracefully(RestartGracefullyInput input) {
        final ListenableFuture<?> f = this.peerRPCs.restartGracefully(input.getSelectionDeferralTime());
        final RpcResultBuilder<RestartGracefullyOutput> rpcResult = RpcResultBuilder.failed();
        Futures.addCallback(f, new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(Object result) {
                        rpcResult.success(new RestartGracefullyOutputBuilder().build()).build();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        rpcResult.withError(ErrorType.RPC, String.format("%s: %s", FAILURE_GRACEFUL_RESTART_MSG,
                                throwable.getMessage()));
                    }
                },
                MoreExecutors.directExecutor());
        try {
            f.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(FAILURE_GRACEFUL_RESTART_MSG, e);
        }
        return Futures.immediateFuture(rpcResult.build());
    }

    @Override
    public ListenableFuture<RpcResult<RouteRefreshRequestOutput>> routeRefreshRequest(
            final RouteRefreshRequestInput input) {
        final ChannelFuture f = sendRRMessage(input);
        if (f != null) {
            return Futures.transform(JdkFutureAdapters.listenInPoolThread(f), input1 -> {
                if (f.isSuccess()) {
                    return RpcResultBuilder.success(new RouteRefreshRequestOutputBuilder().build()).build();
                }
                return RpcResultBuilder.<RouteRefreshRequestOutput>failed().withError(ErrorType.RPC, FAILURE_MSG)
                        .build();
            }, MoreExecutors.directExecutor());
        }
        return RpcResultBuilder.<RouteRefreshRequestOutput>failed().withError(ErrorType.RPC, FAILURE_MSG +
                " due to unsupported address families.").buildFuture();
    }

    private ChannelFuture sendRRMessage(final RouteRefreshRequestInput input) {
        if (!this.supportedFamilies.contains(new TablesKey(input.getAfi(), input.getSafi()))) {
            LOG.info("Unsupported afi/safi: {}, {}.", input.getAfi(), input.getSafi());
            return null;
        }
        final RouteRefresh msg = new RouteRefreshBuilder().setAfi(input.getAfi()).setSafi(input.getSafi()).build();
        return ((BGPSessionImpl) this.session).getLimiter().writeAndFlush(msg);
    }

}
