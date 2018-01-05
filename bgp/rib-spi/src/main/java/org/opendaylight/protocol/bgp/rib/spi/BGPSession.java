/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import io.netty.channel.ChannelInboundHandler;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamilies;

/**
 * BGP Session represents the finite state machine in BGP, including timers and its purpose is to create a BGP
 * connection between BGP speakers. Session is automatically started, when TCP connection is created,
 * but can be stopped manually via close method of the {@link java.io.Closeable} interface.
 * If the session is up, it has to redirect messages to/from user. Handles also malformed messages and unknown requests.
 */
public interface BGPSession extends AutoCloseable, ChannelInboundHandler {
    /**
     * Return the list of tables which the peer has advertised to support.
     *
     * @return Set of tables which it supports.
     */
    @Nonnull
    Set<BgpTableType> getAdvertisedTableTypes();

    /**
     * Return the BGP router ID advertised by the peer.
     *
     * @return Peer's BGP Router ID.
     */
    @Nonnull
    Ipv4Address getBgpId();

    /**
     * Return the AS number which the peer advertises.
     *
     * @return Peer's AS Number
     */
    @Nonnull
    AsNumber getAsNumber();

    /**
     * Return a list with Add Path tables supported advertised and corresponding SendReceive mode.
     *
     * @return AddPathTables supported
     */
    @Nonnull
    List<AddressFamilies> getAdvertisedAddPathTableTypes();

    /**
     * Return the list of tables which the peer has advertised to support.
     * Graceful Restart.
     *
     * @return Set of tables which it supports.
     */
    @Nonnull
    List<BgpTableType> getAdvertisedGracefulRestartTableTypes();

}
