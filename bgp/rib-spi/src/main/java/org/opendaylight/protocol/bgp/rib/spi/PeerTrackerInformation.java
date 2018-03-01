/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Exposes information required from peer to PeerTracker.
 */
public interface PeerTrackerInformation {

    /**
     * Returns PeerID
     *
     * @return PeerID
     */
    @Nonnull
    PeerId getPeerId();

    /**
     * Returns if peer supports Additional Path for specific table.
     *
     * @param tableKey table
     * @return true if Additional Path is supported for defined table
     */
    default boolean supportsAddPathSupported(@Nonnull TablesKey tableKey) {
        final SendReceive sendReceive = getSupportedAddPathTables(tableKey);
        return sendReceive != null && (sendReceive.equals(SendReceive.Both) || sendReceive.equals(SendReceive.Receive));
    }

    /**
     * Returns AddPath support configuration if supported, otherwise null.
     *
     * @param tableKey table
     * @return AddPath support configuration if supported, otherwise null
     */
    @Nullable
    SendReceive getSupportedAddPathTables(@Nonnull TablesKey tableKey);

    /**
     * Returns if peer supports table.
     *
     * @param tableKey table
     * @return true if Additional Path is supported for defined table
     */
    boolean supportsTable(@Nonnull TablesKey tableKey);

    /**
     * Returns YangInstanceIdentifier pointing peer under specific rib.
     *
     * @return Peer YangInstanceIdentifier
     */
    @Nonnull
    YangInstanceIdentifier getPeerRibInstanceIdentifier();

    /**
     * Returns Peer Role.
     *
     * @return PeerRole
     */
    @Nonnull
    PeerRole getRole();
}
