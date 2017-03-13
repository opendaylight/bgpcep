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
import io.netty.channel.ChannelFuture;
import java.util.Set;
import java.util.concurrent.Future;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev160322.BgpPeerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev160322.RouteRefreshRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpPeerRpc implements BgpPeerRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(BgpPeerRpc.class);
    private static final String FAILURE_MSG = "Failed to send Route Refresh message";

    private final BGPSession session;
    private final Set<TablesKey> supportedFamilies;

    BgpPeerRpc(final BGPSession session, final Set<TablesKey> supportedFamilies) {
        this.session = Preconditions.checkNotNull(session);
        this.supportedFamilies = Preconditions.checkNotNull(supportedFamilies);
    }

    @Override
    public Future<RpcResult<Void>> routeRefreshRequest(final RouteRefreshRequestInput input) {
        final ChannelFuture f = sendRRMessage(input);
        if (f != null) {
            return Futures.transform(JdkFutureAdapters.listenInPoolThread(f), (Function<Void, RpcResult<Void>>) input1 -> {
                if (f.isSuccess()) {
                    return RpcResultBuilder.<Void>success().build();
                } else {
                    return RpcResultBuilder.<Void>failed().withError(ErrorType.RPC, FAILURE_MSG).build();
                }
            });
        }
        return RpcResultBuilder.<Void>failed().withError(ErrorType.RPC, FAILURE_MSG + " due to unsupported address families.").buildFuture();
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
