/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;

import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.state.spi.BGPSessionStateListener;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yangtools.yang.binding.Notification;

public abstract class BGPPeerState implements BGPSessionStateListener {
    @Override
    public void messageSent(final Notification msg) {
        //TODO
    }

    @Override
    public void messageReceived(final Notification msg) {
        //TODO
    }

    @Override
    public void advertizeCapabilities(final int holdTimerValue, final SocketAddress remoteAddress,
        final SocketAddress localAddress, final Set<BgpTableType> tableTypes, final List<BgpParameters> bgpParameters) {
        //TODO
    }

    @Override
    public void setSessionState(final SessionState state) {
        //TODO
    }
}
