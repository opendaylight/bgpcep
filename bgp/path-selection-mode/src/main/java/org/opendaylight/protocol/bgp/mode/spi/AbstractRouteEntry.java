/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.spi;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRouteEntry<T extends BestPath> implements RouteEntry {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRouteEntry.class);

    protected static final NodeIdentifier ROUTES_IDENTIFIER = new NodeIdentifier(Routes.QNAME);
    protected final BGPPeerTracker peerTracker;

    public AbstractRouteEntry(final BGPPeerTracker peerTracker) {
        this.peerTracker = requireNonNull(peerTracker);
    }

    /**
     * Create value.
     *
     * @param routeId router ID pathArgument
     * @param path    BestPath
     * @return MapEntryNode
     */
    public abstract MapEntryNode createValue(NodeIdentifierWithPredicates routeId, T path);

    protected static void fillLocRib(final YangInstanceIdentifier routeTarget, final NormalizedNode<?, ?> value,
            final DOMDataWriteTransaction tx) {
        if (value != null) {
            LOG.debug("Write route to LocRib {}", value);
            tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget, value);
        } else {
            LOG.debug("Delete route from LocRib {}", routeTarget);
            tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
        }
    }

    protected static void update(final PeerId destPeer, final YangInstanceIdentifier routeTarget,
            final Optional<ContainerNode> effAttr, final NormalizedNode<?, ?> value, final RIBSupport ribSup,
            final DOMDataWriteTransaction tx) {
        if (!writeRoute(destPeer, routeTarget, effAttr, value, ribSup, tx)) {
            deleteRoute(destPeer, routeTarget, tx);
        }
    }

    protected static boolean writeRoute(final PeerId destPeer, final YangInstanceIdentifier routeTarget,
            final Optional<ContainerNode> effAttrib, final NormalizedNode<?, ?> value, final RIBSupport ribSup,
            final DOMDataWriteTransaction tx) {
        if (effAttrib.isPresent() && value != null) {
            LOG.debug("Write route {} to peer AdjRibsOut {}", value, destPeer);
            tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget, value);
            tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget.node(ribSup.routeAttributesIdentifier()),
                    effAttrib.get());
            return true;
        }
        return false;
    }

    private static void deleteRoute(final PeerId destPeer, final YangInstanceIdentifier routeTarget,
            final DOMDataWriteTransaction tx) {
        LOG.trace("Removing {} from transaction for peer {}", routeTarget, destPeer);
        tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
    }

    protected final boolean filterRoutes(final PeerId fromPeerId, final PeerId destPeer, final TablesKey localTK) {
        final Peer peer = this.peerTracker.getPeer(destPeer);
        return !(peer == null
                || !peer.supportsTable(localTK)
                || PeerRole.Internal.equals(peer.getRole()))
                && !fromPeerId.equals(destPeer);
    }

    protected final YangInstanceIdentifier getAdjRibOutYII(final RouteEntryDependenciesContainer entryDep,
            final YangInstanceIdentifier rootPath, final PathArgument routeId) {
        return entryDep.getRibSupport().routePath(rootPath.node(AdjRibOut.QNAME).node(Tables.QNAME)
            .node(RibSupportUtils.toYangTablesKey(entryDep.getLocalTablesKey())).node(ROUTES_IDENTIFIER),
                routeId);
    }
}
