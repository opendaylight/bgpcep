/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.protocol.bgp.rib.impl.stats.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;

/**
 * @author Kevin Wang
 */
public interface BGPRenderStats {
    BgpRenderState getBgpRenderState();

    PerTableTypeRouteCounter getLocRibRouteCounter();

    UnsignedInt32Counter getConfiguredPeerCounter();

    UnsignedInt32Counter getConnectedPeerCounter();
}
