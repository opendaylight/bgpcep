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

final class BasePathSelection implements PathSelectionMode {
    @Override
    public RouteEntry createRouteEntry(final YangInstanceIdentifier writePath, final RIBSupport ribSupport, final ExportPolicyPeerTracker exportPolicyPeerTracker,
        final TablesKey localTablesKey, final CacheDisconnectedPeers cacheDisconnectedPeers) {
        return ribSupport.isComplexRoute() ? new BaseComplexRouteEntry(writePath, ribSupport, exportPolicyPeerTracker, localTablesKey, cacheDisconnectedPeers) :
            new BaseSimpleRouteEntry(writePath, ribSupport, exportPolicyPeerTracker, localTablesKey, cacheDisconnectedPeers);
    }

    @Override
    public void close() throws Exception {
        //no-op
    }
}
