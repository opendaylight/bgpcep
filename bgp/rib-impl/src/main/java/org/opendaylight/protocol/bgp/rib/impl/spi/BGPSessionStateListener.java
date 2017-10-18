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
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;


/**
 * BGP Operational Session State Listener
 */
public interface BGPSessionStateListener extends BGPMessagesListener{
    /**
     * Advertize Session capabilities
     *
     * @param holdTimerValue hold Timer
     * @param remoteAddress remote Address
     * @param localAddress local Address
     * @param tableTypes supported families
     * @param bgpParameters bgp capabilities
     */
    void advertizeCapabilities(final int holdTimerValue, @Nonnull final SocketAddress remoteAddress,
        @Nonnull final SocketAddress localAddress, @Nonnull final Set<BgpTableType> tableTypes,
        @Nonnull List<BgpParameters> bgpParameters);

    /**
     * Fired when session state changes
     *
     * @param state session state
     */
    void setSessionState(@Nonnull State state);
}
