/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import io.netty.channel.ChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.parser.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerConstraint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;

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
     * Return advertised graceful capability containing the list of tables which the peer has advertised to support,
     * restart time and restarting flags.
     *
     * @return Advertised graceful restart capability.
     */
    @Nonnull
    default GracefulRestartCapability getAdvertisedGracefulRestartCapability() {
        return GracefulRestartUtil.EMPTY_GR_CAPABILITY;
    }

    /**
     * Return advertised long-lived graceful capability containing the list of tables with stale time which
     * the peer has advertised to support.
     *
     * @return Advertised long-lived graceful restart capability.
     */
    @Nonnull
    default LlGracefulRestartCapability getAdvertisedLlGracefulRestartCapability() {
        return GracefulRestartUtil.EMPTY_LLGR_CAPABILITY;
    }

    /**
     * Close peer session without sending Notification message.
     */
    void closeWithoutMessage();

    /**
     * Add peer constraint to session pipeline decoder.
     */
    <T extends PeerConstraint> void addDecoderConstraint(Class<T> constraintClass, T constraint);

    /**
     * Schedule a task to be executed in the context of the session handling thread.
     *
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return Future representing the scheduled task.
     */
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
}
