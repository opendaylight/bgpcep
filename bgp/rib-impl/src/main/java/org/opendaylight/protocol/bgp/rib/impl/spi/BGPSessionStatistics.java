/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;

/**
 * Serves to expose BGP session statistics to the BGP Peer, to which the session is related.
 */
public interface BGPSessionStatistics {

    /**
     * Retrieves actual BGP session state. Containing all information collected from the session.
     *
     * @return State of the BGP session.
     */
    BgpSessionState getBgpSesionState();

    /**
     * Resets BGP session statistics. Sets counters values to zero.
     */
    void resetSessionStats();
}
