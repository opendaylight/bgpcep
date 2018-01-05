/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;

/**
 * Marker interface identifying a BGP peer.
 */
public interface Peer {
    /**
     * Return peer's symbolic name.
     *
     * @return symbolic name.
     */
    @Nonnull
    String getName();

    /**
     * Return the peer's BGP identifier as raw byte array.
     *
     * @return byte[] raw identifier
     */
    byte[] getRawIdentifier();

    /**
     * Close Peers and performs asynchronously DS clean up.
     *
     * @return future
     */
    @Nonnull
    ListenableFuture<?> close();
}
