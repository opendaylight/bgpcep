/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;

/**
 * BGP Operational Session State Listener.
 */
public interface BGPSessionStateListener extends BGPMessagesListener {
    /**
     * Advertize Session capabilities.
     *
     * @param holdTimerValue hold Timer
     * @param remoteAddress remote Address
     * @param localAddress local Address
     * @param tableTypes supported families
     * @param bgpParameters bgp capabilities
     */
    void advertizeCapabilities(int holdTimerValue, @NonNull SocketAddress remoteAddress,
        @NonNull SocketAddress localAddress, @NonNull Set<BgpTableType> tableTypes,
        @NonNull List<BgpParameters> bgpParameters);

    /**
     * Fired when session state changes.
     *
     * @param state session state
     */
    void setSessionState(@NonNull State state);
}
