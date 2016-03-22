/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rpc.rev160322.BgpRibRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rpc.rev160322.RouteRefreshRequestInput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class BgpRibRpc implements BgpRibRpcService {

    @Override
    public Future<RpcResult<Void>> routeRefreshRequest(final RouteRefreshRequestInput input) {
        sendRRMessage(input);
        return null;
    }

    private void sendRRMessage(final RouteRefreshRequestInput input) {
        // identify peer from input
        input.getPeerRef().getValue();
        // get his session instance --- BGPSessionImpl.writeAndFlush(msg) / alebo len write
        new RouteRefreshBuilder().setAfi(input.getAfi()).setSafi(input.getSafi()).build();
    }

}
