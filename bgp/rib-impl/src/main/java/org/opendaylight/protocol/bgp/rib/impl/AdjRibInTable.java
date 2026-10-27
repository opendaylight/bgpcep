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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

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
            .node(ADJRIBIN_NID).node(TABLES_NID).node(ribSupport.ribSupport().tablesKey())
            .build(), ribSupport);
    }

    YangInstanceIdentifier routesPath() {
        return ribSupport.ribSupport().routesPath(path);
    }
}
