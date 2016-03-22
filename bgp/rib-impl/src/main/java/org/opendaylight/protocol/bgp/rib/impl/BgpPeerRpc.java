/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelFuture;
import java.util.Map;
import java.util.concurrent.Future;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rpc.rev160322.BgpPeerRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rpc.rev160322.RouteRefreshRequestInput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class BgpPeerRpc implements BgpPeerRpcService {

    private final BGPSession session;
    private final Map<TablesKey, AdjRibOutListener> adjRibOutListenerSet;

    BgpPeerRpc(final BGPSession session, final Map<TablesKey, AdjRibOutListener> adjRibOutListenerSet) {
        this.session = Preconditions.checkNotNull(session);
        this.adjRibOutListenerSet = Preconditions.checkNotNull(adjRibOutListenerSet);
    }

    @Override
    public Future<RpcResult<Void>> routeRefreshRequest(final RouteRefreshRequestInput input) {
        final ChannelFuture f = sendRRMessage(input);
        if (f != null && f.isSuccess()) {

        } else {
            // f is null
        }
//        Futures.transform(, sendRRMessage(input));
        return null; // TODO transform f. check success
    }

    private ChannelFuture sendRRMessage(final RouteRefreshRequestInput input) {
        final AdjRibOutListener listener = this.adjRibOutListenerSet.get(new TablesKey(input.getAfi(), input.getSafi()));
        if (listener == null) {
            return null;
        }
        final RouteRefresh msg = new RouteRefreshBuilder().setAfi(input.getAfi()).setSafi(input.getSafi()).build();
        return ((BGPSessionImpl) this.session).getLimiter().writeAndFlush(msg);
    }

}
