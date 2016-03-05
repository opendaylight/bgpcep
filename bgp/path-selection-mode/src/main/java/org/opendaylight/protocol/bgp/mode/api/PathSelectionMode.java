/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.api;

import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.ExportPolicyPeerTracker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public interface PathSelectionMode {
    /**
     * Create a RouteEntry
     *
     * @param writePath
     * @param isComplexRoute true if route is complex
     * @param localTablesKey
     * @param cacheDisconnectedPeers
     * @return ComplexRouteEntry if is complex otherwise a SimpleRouteEntry
     */
    RouteEntry createRouteEntry(YangInstanceIdentifier writePath, RIBSupport isComplexRoute, ExportPolicyPeerTracker exportPolicyPeerTracker,
        TablesKey localTablesKey, CacheDisconnectedPeers cacheDisconnectedPeers);
}
