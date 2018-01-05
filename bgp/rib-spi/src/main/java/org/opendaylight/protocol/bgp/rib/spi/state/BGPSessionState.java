/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.State;

/**
 * BGP Operational Session State.
 */
public interface BGPSessionState {
    /**
     * Internal session state.
     *
     * @return Internal session state
     */
    @Nonnull
    State getSessionState();

    /**
     * Additional Path capability.
     *
     * @return true if supported
     */
    boolean isAddPathCapabilitySupported();

    /**
     * AS 4 Bytes capability.
     *
     * @return true if supported
     */
    boolean isAsn32CapabilitySupported();

    /**
     * Graceful Restart.
     *
     * @return true if supported
     */
    boolean isGracefulRestartCapabilitySupported();

    /**
     * Multiprotocol capability.
     *
     * @return true if supported
     */
    boolean isMultiProtocolCapabilitySupported();

    /**
     * Router Refresh Capability.
     *
     * @return true if supported
     */
    boolean isRouterRefreshCapabilitySupported();
}
