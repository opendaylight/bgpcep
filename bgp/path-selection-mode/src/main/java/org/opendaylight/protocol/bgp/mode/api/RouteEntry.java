/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.api;

import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A single route entry inside a route table. Maintains the attributes of
 * from all contributing peers. The information is stored in arrays with a
 * shared map of offsets for peers to allow lookups. This is needed to
 * maintain low memory overhead in face of large number of routes and peers,
 * where individual object overhead becomes the dominating factor.
 */
public interface RouteEntry {
    /**
     * Remove route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @return return true if it was the last route on entry
     */
    boolean removeRoute(UnsignedInteger routerId, Long remotePathId);

    /**
     * Indicates whether best has changed.
     *
     * @param localAs The local autonomous system number
     * @return return true if it has changed
     */
    boolean selectBest(long localAs);

    /**
     * Add Route.
     *
     * @param routerId     router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @param attrII       route Attributes Identifier
     * @param data         route Data change
     * @return returns the offset
     */
    int addRoute(UnsignedInteger routerId, Long remotePathId, NodeIdentifier attrII, NormalizedNode<?, ?> data);

    /**
     * Update LocRibOut and AdjRibsOut by removing stale best path and writing new best.
     *
     * @param entryDependencies entry Dependencies container
     * @param routeIdPA         router ID pathArgument
     * @param tx                DOM transaction
     */
    void updateBestPaths(
            @Nonnull RouteEntryDependenciesContainer entryDependencies,
            @Nonnull NodeIdentifierWithPredicates routeIdPA,
            @Nonnull DOMDataWriteTransaction tx);

    /**
     * Initialize LocRibOut and AdjRibsOut for new peers with already present best paths.
     *
     * @param entryDependencies Route Entry Dependencies wrapper
     * @param entryInfo         Route Entry Info wrapper
     * @param tx                transaction
     */
    void initializeBestPaths(
            @Nonnull RouteEntryDependenciesContainer entryDependencies,
            @Nonnull RouteEntryInfo entryInfo,
            @Nullable PeerExportGroup peerGroup,
            @Nonnull DOMDataWriteTransaction tx);
}