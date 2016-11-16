/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import org.opendaylight.controller.config.yang.bgp.rib.impl.RIBImplRuntimeMXBean;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.protocol.bgp.state.spi.counters.UnsignedInt32Counter;

/**
 * @author Kevin Wang
 */
public interface BGPRenderStats extends RIBImplRuntimeMXBean {

    @Deprecated
    PerTableTypeRouteCounter getLocRibRouteCounter();

    UnsignedInt32Counter getConfiguredPeerCounter();

    UnsignedInt32Counter getConnectedPeerCounter();
}
