/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer;

import java.util.concurrent.atomic.LongAdder;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;

public interface BGPPeerStats {
    BgpPeerState getBgpPeerState();

    PerTableTypeRouteCounter getAdjRibInRouteCounters();

    PerTableTypeRouteCounter getAdjRibOutRouteCounters();

    LongAdder getSessionEstablishedCounter();
}
