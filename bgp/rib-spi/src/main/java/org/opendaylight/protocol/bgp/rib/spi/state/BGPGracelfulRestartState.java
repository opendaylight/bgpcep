/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

/**
 * BGP Operational Graceful Restart State
 */
public interface BGPGracelfulRestartState {
    /**
     * is Graceful Restart Supported advertized to neighbor
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertized to neighbor
     */
    boolean isGracefulRestartAdvertized(@Nonnull TablesKey tablesKey);

    /**
     * is Graceful Restart Supported advertized by neighbor
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertized by neighbor
     */
    boolean isGracefulRestartReceived(TablesKey tablesKey);

    /**
     * This flag indicates whether the local neighbor is currently restarting
     *
     * @return local restarting state
     */
    boolean isLocalRestarting();

    /**
     * The period of time (advertised by the peer) that the peer expects a restart of a
     * BGP session to take
     *
     * @return time
     */
    int getPeerRestartTime();

    /**
     * This flag indicates whether the remote neighbor is currently in the process of
     * restarting, and hence received routes are currently stale
     *
     * @return peer is restarting
     */
    boolean isPeerRestarting();
}
