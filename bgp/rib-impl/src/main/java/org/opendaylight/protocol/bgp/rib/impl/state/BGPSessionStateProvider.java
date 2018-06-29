/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;

import org.opendaylight.protocol.bgp.rib.impl.spi.BGPMessagesListener;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;

/**
 * Serves to expose BGP Session, Timers, Transport, BGPErrorHandlingState Operational State.
 */
public interface BGPSessionStateProvider {
    /**
     * BGP Operational Session State.
     *
     * @return BGPSessionState
     */
    BGPSessionState getBGPSessionState();

    /**
     * BGP Operational Timers State.
     *
     * @return BGPTimersState
     */
    BGPTimersState getBGPTimersState();

    /**
     * BGP Operational Transport State.
     *
     * @return BGPTransportState
     */
    BGPTransportState getBGPTransportState();

    /**
     * Register BGP Operational Messages State Listener.
     *
     * @param bgpMessagesListener BGPMessagesListener
     */
    void registerMessagesCounter(BGPMessagesListener bgpMessagesListener);
}
