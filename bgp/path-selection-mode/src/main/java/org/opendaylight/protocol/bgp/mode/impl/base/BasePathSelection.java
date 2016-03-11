/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.base;

import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.ExportPolicyPeerTracker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

final class BasePathSelection implements PathSelectionMode {
    @Override
    public RouteEntry createRouteEntry(final PathArgument routeId, final YangInstanceIdentifier writePath, final RIBSupport ribSup,
        final ExportPolicyPeerTracker exportPPT, final TablesKey localTk, final CacheDisconnectedPeers cacheDP) {
        return ribSup.isComplexRoute() ? new BaseComplexRouteEntry(routeId, writePath, ribSup, exportPPT, localTk, cacheDP) :
            new BaseSimpleRouteEntry(routeId, writePath, ribSup, exportPPT, localTk, cacheDP);
    }

    @Override
    public void close() throws Exception {
        //no-op
    }
}
