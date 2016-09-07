/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.spi;

import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Represent monitored router's peer. Is responsible of processing per-peer messages.
 *
 */
public interface BmpRouterPeer extends AutoCloseable {

    /**
     * Process peer-peer message that is related to this peer.
     * @param message BMP Per-peer message.
     */
    void onPeerMessage(Notification message);

}
