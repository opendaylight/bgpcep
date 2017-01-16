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
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

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
     * Return the peer's BGP identifier as raw bytearray
     *
     * @return byte[] raw identifier
     */
    byte[] getRawIdentifier();

    /**
     * Returns PeerID
     *
     * @return PeerID
     */
    @Nonnull
    PeerId getPeerId();

    /**
     * Returns if peer supports Additional Path for spectific table
     *
     * @param tableKey table
     * @return true if Additional Path is supported for defined table
     */
    default boolean supportsAddPathSupported(@Nonnull TablesKey tableKey) {
        final SendReceive sendReceive = getSupportedAddPathTables(tableKey);
        return sendReceive != null && (sendReceive.equals(SendReceive.Both) || sendReceive.equals(SendReceive.Receive));
    }

    /**
     * Returns AddPath support configuration if supported, otherwise null
     *
     * @param tableKey table
     * @return AddPath support configuration if supported, otherwise null
     */
    @Nullable
    SendReceive getSupportedAddPathTables(@Nonnull TablesKey tableKey);

    /**
     * Returns if peer supports table
     *
     * @param tableKey table
     * @return true if Additional Path is supported for defined table
     */
    boolean supportsTable(@Nonnull TablesKey tableKey);

    /**
     * Peer YangInstanceIdentifier
     *
     * @return Peer YangInstanceIdentifier
     */
    @Nonnull
    YangInstanceIdentifier getYii();

    /**
     * Returns Peer Role
     *
     * @return PeerRole
     */
    @Nonnull
    PeerRole getRole();

    /**
     * Close Peers and performs asynchronously DS clean up
     *
     * @return future
     */
    @Nonnull
    ListenableFuture<?> close();

    /**
     * Returns if peer structure has been created and table is supported by peer
     * @param tablesKey table
     * @return true if supported and peer's structure has been created
     */
    boolean isTableSupportedAndReady(TablesKey tablesKey);
}
