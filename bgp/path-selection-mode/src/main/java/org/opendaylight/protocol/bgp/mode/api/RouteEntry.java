/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.api;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RouteUpdateKey;
import org.opendaylight.protocol.bgp.rib.spi.policy.PeerExportGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
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
     * Remove route
     *
     * @param routerId router ID in unsigned integer format from an Ipv4Address
     * @return return true if it was the last route on entry
     */
    boolean removeRoute(UnsignedInteger routerId);

    /**
     * Create value
     *
     * @param routeId router ID pathArgument
     * @param path    BestPath
     * @return MapEntryNode
     */
    MapEntryNode createValue(PathArgument routeId, BestPath path);

    /**
     * Indicates whether best has changed
     *
     * @param localAs The local autonomous system number
     * @return return true if it has changed
     */
    boolean selectBest(long localAs);

    /**
     * @param routerId             router ID in unsigned integer format from an Ipv4Address
     * @param attributesIdentifier route Attributes Identifier
     * @param data                 route Data change
     * @return returns the offset
     */
    int addRoute(UnsignedInteger routerId, NodeIdentifier attributesIdentifier, NormalizedNode<?, ?> data);

    /**
     * Update LocRibOut and AdjRibsOut by removing stale best path and writing new best
     * @param tx
     * @param routeUpdateKey router ID pathArgument
     */
    void updateRoute(DOMDataWriteTransaction tx, RouteUpdateKey routeUpdateKey);

    /**
     * Write Route on LocRibOut and AdjRibsOut
     *
     * @param peerId destination peerId
     * @param routeId
     * @param rootPath
     * @param peerGroup
     * @param tx
     */
    void writeRoute(PeerId peerId, PathArgument routeId, YangInstanceIdentifier rootPath, PeerExportGroup peerGroup, DOMDataWriteTransaction tx);
}