/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Exposes information required from peer to PeerTracker.
 */
public interface PeerTrackerInformation {

    /**
     * Returns Peer id.
     *
     * @return PeerID
     */
    @NonNull PeerId getPeerId();

    /**
     * Returns if peer supports Additional Path for specific table.
     *
     * @param tableKey table
     * @return true if Additional Path is supported for defined table
     */
    default boolean supportsAddPathSupported(final @NonNull TablesKey tableKey) {
        final SendReceive sendReceive = getSupportedAddPathTables(tableKey);
        return sendReceive != null && (sendReceive.equals(SendReceive.Both) || sendReceive.equals(SendReceive.Receive));
    }

    /**
     * Returns AddPath support configuration if supported, otherwise null.
     *
     * @param tableKey table
     * @return AddPath support configuration if supported, otherwise null
     */
    @Nullable SendReceive getSupportedAddPathTables(@NonNull TablesKey tableKey);

    /**
     * Returns true if we have advertized support for a table, i.e. any prefix from this table should
     * be subject to export towards the peer.
     *
     * @param tableKey table
     * @return true if the table is being advertized to the peer.
     */
    boolean supportsTable(@NonNull TablesKey tableKey);

    /**
     * Creates Table Adj Rib Out Instance identifier.
     *
     * @param tablekey table key
     * @return instance identifier.
     */
    @NonNull KeyedInstanceIdentifier<Tables, TablesKey> getRibOutIId(@NonNull TablesKey tablekey);

    /**
     * Returns Peer Role.
     *
     * @return PeerRole
     */
    @NonNull PeerRole getRole();

    /**
     * Returns Cluster Id.
     *
     * @return Cluster Id
     */
    @Nullable ClusterIdentifier getClusterId();

    /**
     * Returns Local AS.
     *
     * @return AS
     */
    @Nullable AsNumber getLocalAs();
}
