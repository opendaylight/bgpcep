/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;

/**
 * Contains all Peer RPC methods related.
 */
public interface PeerRPCs {
    /**
     * Release Peer session.
     */
    @Nonnull
    ListenableFuture<?> releaseConnection();
}
