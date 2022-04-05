/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * BGP Operational Messages Listener State.
 */
@NonNullByDefault
public interface BGPMessagesListener {
    /**
     * Fired when message is sent.
     *
     * @param msg message
     */
    void messageSent(Notification<?> msg);

    /**
     * Fired when message is received.
     *
     * @param msg message
     */
    void messageReceived(Notification<?> msg);
}
