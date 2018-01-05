/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

/**
 * BGP Peer Messages Operational State.
 */
public interface BGPPeerMessagesState {
    /**
     * Update Messages Sent count.
     *
     * @return count
     */
    long getUpdateMessagesSentCount();

    /**
     * Notification Messages Sent count.
     *
     * @return count
     */
    long getNotificationMessagesSentCount();

    /**
     * Update Messages Received count.
     *
     * @return count
     */
    long getUpdateMessagesReceivedCount();

    /**
     * Notification Update Messages Received count.
     *
     * @return count
     */
    long getNotificationMessagesReceivedCount();
}
