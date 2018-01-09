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
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
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
     * @param remotePathId remote path Id received
     * @return return true if it was the last route on entry
     */
    boolean removeRoute(UnsignedInteger routerId, Long remotePathId);

    /**
     * Create value
     *
     * @param routeId router ID pathArgument
     * @param path BestPath
     * @return MapEntryNode
     */
    @Deprecated
    MapEntryNode createValue(PathArgument routeId, BestPath path);

    /**
     * Indicates whether best has changed
     *
     * @param localAs The local autonomous system number
     * @return return true if it has changed
     */
    boolean selectBest(long localAs);

    /**
     * @param routerId router ID in unsigned integer format from an Ipv4Address
     * @param remotePathId remote path Id received
     * @param attrII route Attributes Identifier
     * @param data route Data change
     * @return returns the offset
     */
    int addRoute(UnsignedInteger routerId, Long remotePathId, NodeIdentifier attrII, NormalizedNode<?, ?> data);

    /**
     * Update LocRibOut and AdjRibsOut by removing stale best path and writing new best
     *
     * @param localTK local Table Key
     * @param peerPT peer export policy
     * @param locRibTarget YII local rib
     * @param ribSupport rib support
     * @param tx DOM transaction
     * @param routeIdPA router ID pathArgument
     */
    void updateRoute(TablesKey localTK, ExportPolicyPeerTracker peerPT, YangInstanceIdentifier locRibTarget, RIBSupport ribSupport,
        DOMDataWriteTransaction tx, PathArgument routeIdPA);

    /**
     * Write Route on LocRibOut and AdjRibsOut
     *  @param peerId destination peerId
     * @param routeId router ID path Argument
     * @param rootPath YII root path
     * @param peerGroup PeerExportGroup
     * @param localTK local Table Key
     * @param ribSupport rib support
     * @param tx DOM transaction
     */
    void writeRoute(PeerId peerId, PathArgument routeId, YangInstanceIdentifier rootPath, PeerExportGroup peerGroup, TablesKey localTK,
        ExportPolicyPeerTracker peerPT, RIBSupport ribSupport, DOMDataWriteTransaction tx);
}