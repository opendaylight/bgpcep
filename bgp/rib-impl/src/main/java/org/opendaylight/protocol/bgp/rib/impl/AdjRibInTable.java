/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import java.util.Collection;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * The combination of a peer adj-rib-in table path and its corresponding {@link RIBSupportContext}.
 *
 * @param path adj-rib-in table path
 * @param ribSupport the {@link RIBSupportContext}
 */
@NonNullByDefault
record AdjRibInTable(YangInstanceIdentifier path, RIBSupportContext ribSupport) implements Immutable {
    @Deprecated
    AdjRibInTable {
        requireNonNull(path);
        requireNonNull(ribSupport);
    }

    static AdjRibInTable ofPeer(final RIBSupportContext ribSupport, final YangInstanceIdentifier peerPath) {
        return new AdjRibInTable(YangInstanceIdentifier.builder(peerPath)
            .node(ADJRIBIN_NID).node(TABLES_NID).node(ribSupport.getRibSupport().tablesKey())
            .build(), ribSupport);
    }

    void createEmptyTableStructure(final DOMDataTreeWriteTransaction tx) {
        ribSupport.createEmptyTableStructure(tx, path);
    }

    void removeTable(final DOMDataTreeWriteTransaction tx) {
        tx.delete(LogicalDatastoreType.OPERATIONAL, path);
    }

    Collection<NodeIdentifierWithPredicates> writeRoutes(final DOMDataTreeWriteTransaction tx, final MpReachNlri nlri,
            final Attributes attributes) {
        return ribSupport.writeRoutes(tx, path, nlri, attributes);
    }

    void removeRoutes(final DOMDataTreeWriteTransaction tx, final MpUnreachNlri nlri) {
        ribSupport.deleteRoutes(tx, path, nlri);
    }

    YangInstanceIdentifier routesPath() {
        return ribSupport.getRibSupport().routesPath(path);
    }

    YangInstanceIdentifier routePath(final PathArgument routeId) {
        return ribSupport.getRibSupport().routePath(path, routeId);
    }
}
