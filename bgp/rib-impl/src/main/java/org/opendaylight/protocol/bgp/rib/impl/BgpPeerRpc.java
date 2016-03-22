/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.ChannelFuture;
import java.util.Set;
import java.util.concurrent.Future;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rpc.rev160322.BgpPeerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rpc.rev160322.RouteRefreshRequestInput;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class BgpPeerRpc implements BgpPeerRpcService {

    private final BGPSession session;
    private final Set<TablesKey> supportedFamilies;

    BgpPeerRpc(final BGPSession session, final Set<TablesKey> supportedFamilies) {
        this.session = Preconditions.checkNotNull(session);
        this.supportedFamilies = Preconditions.checkNotNull(supportedFamilies);
    }

    @Override
    public Future<RpcResult<Void>> routeRefreshRequest(final RouteRefreshRequestInput input) {
        final ChannelFuture f = sendRRMessage(input);
        final ListenableFuture<Void> listenableF = JdkFutureAdapters.listenInPoolThread(f);
        if (listenableF != null) {
            return Futures.transform(listenableF, new Function<Void, RpcResult<Void>>() {
                @Override
                public RpcResult<Void> apply(final Void input) {
                    if (f.isSuccess()) {
                        return RpcResultBuilder.<Void>success().build();
                    } else {
                        return RpcResultBuilder.<Void>failed().withError(ErrorType.RPC, "Failed to send Route Refresh message.").build();
                    }
                }
            });
        }
        return Futures.transform(listenableF, new Function<Void, RpcResult<Void>>() {
            @Override
            public RpcResult<Void> apply(final Void input) {
                return RpcResultBuilder.<Void>failed().withError(ErrorType.RPC, "Failed to send Route Refresh message due to unsupported address families.").build();
            }
        });
    }

    private ChannelFuture sendRRMessage(final RouteRefreshRequestInput input) {
        if (this.supportedFamilies.contains(new TablesKey(input.getAfi(), input.getSafi()))) {
            return null;
        }
        final RouteRefresh msg = new RouteRefreshBuilder().setAfi(input.getAfi()).setSafi(input.getSafi()).build();
        return ((BGPSessionImpl) this.session).getLimiter().writeAndFlush(msg);
    }

}
