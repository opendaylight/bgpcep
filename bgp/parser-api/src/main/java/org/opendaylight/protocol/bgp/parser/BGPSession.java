/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.util.Set;

import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * BGP Session represents the finite state machine in BGP, including timers and its purpose is to create a BGP
 * connection between BGP speakers. Session is automatically started, when TCP connection is created, but can be stopped
 * manually via close method of the Closeable interface.
 *
 * If the session is up, it has to redirect messages to/from user. Handles also malformed messages and unknown requests.
 */
public interface BGPSession extends ProtocolSession<Notification> {
    /**
     * Return the list of tables which the peer has advertized to support.
     *
     * @return Set of tables which it supports.
     */

    Set<BgpTableType> getAdvertisedTableTypes();

    /**
     * Return the BGP router ID advertized by the peer.
     *
     * @return Peer's BGP Router ID.
     */
    Ipv4Address getBgpId();

    /**
     * Return the AS number which the peer advertizes.
     *
     * @return Peer's AS Number
     */
    AsNumber getAsNumber();
}
