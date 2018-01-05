/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nullable;

/**
 * Interface for acquiring BGP Peer State.
 */
public interface BGPPeerStateConsumer {
    /**
     * Returns Peer Operational State.
     *
     * @return BGP Peer State
     */
    @Nullable
    BGPPeerState getPeerState();
}
