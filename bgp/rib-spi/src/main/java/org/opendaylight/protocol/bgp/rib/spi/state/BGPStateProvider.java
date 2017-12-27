/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nonnull;

/**
 * Provider of BGP Operational state.
 */
public interface BGPStateProvider {
    /**
     * register Rib state.
     *
     * @param bgpState rib State
     */
    void bind(@Nonnull BGPRibStateConsumer bgpState);

    /**
     * Unregister Rib state.
     *
     * @param bgpState Rib/Peer State
     */
    void unbind(@Nonnull BGPRibStateConsumer bgpState);

    /**
     * registerRib/Peer state.
     *
     * @param bgpState rib State
     */
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    void bind(@Nonnull BGPPeerStateConsumer bgpState);

    /**
     * Unregister Peer state.
     *
     * @param bgpState Peer State
     */
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    void unbind(@Nonnull BGPPeerStateConsumer bgpState);
}
