/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.state;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Binds BGP operational state providers.
 */
@NonNullByDefault
public interface BGPStateProviderConsumer {
    /**
     * Register rib state provider.
     *
     * @param ribStateProvider rib state provider
     */
    void bind(BGPRibStateProvider ribStateProvider);

    /**
     * Register peer state provider.
     *
     * @param peerStateProvider peer state provider
     */
    void bind(BGPPeerStateProvider peerStateProvider);

    /**
     * Unregister rib state provider.
     *
     * @param ribStateProvider rib state provider
     */
    void unbind(BGPRibStateProvider ribStateProvider);

    /**
     * Unregister peer state provider.
     *
     * @param peerStateProvider peer State
     */
    void unbind(BGPPeerStateProvider peerStateProvider);
}
