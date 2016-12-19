/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;


import java.util.List;

/**
 * Provides list with Operational State of BGP RIBs
 */
public interface BGPStateCollector {
    /**
     * List of Registered BGP Rib States
     * @return ribs stats
     */
    List<BGPRIBState> getRibStats();

    /**
     * register Rib state
     * @param bgpRibState rib State
     */
    public void bind(BGPRIBState bgpRibState);

    /**
     * Unregister Rib state
     * @param bgpRibState rib State
     */
    public void unbind(BGPRIBState bgpRibState);
}
