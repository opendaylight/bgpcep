/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi;

import io.netty.channel.Channel;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * BGP Session State Listener
 */
public interface BGPSessionStateListener {
    /**
     * Fired when message is sent.
     *
     * @param msg message
     */
    void messageSent(@Nonnull Notification msg);

    /**
     * Fired when message is received.
     *
     * @param msg message
     */
    void messageReceived(@Nonnull Notification msg);

    /**
     * Advertize Session capabilities
     *
     * @param holdTimerValue hold Timer
     * @param remoteAddress remote Address
     * @param tableTypes supported families
     * @param bgpParameters bgp capabilities
     */
    void advertizeCapabilities(final int holdTimerValue, @Nonnull final Channel remoteAddress,
        @Nonnull final Set<BgpTableType> tableTypes, @Nonnull List<BgpParameters> bgpParameters);

    /**
     * Fired when session state changes
     *
     * @param state session state
     */
    void setSessionState(@Nonnull SessionState state);
}
