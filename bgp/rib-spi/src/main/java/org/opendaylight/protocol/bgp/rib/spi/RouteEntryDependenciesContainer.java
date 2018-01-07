/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Container wrapper for all dependencies related to Route Entry, required for process and storage.
 */
public interface RouteEntryDependenciesContainer {
    /**
     * Returns rib support.
     *
     * @return RIBSupport
     */
    @Nonnull
    RIBSupport getRibSupport();


    /**
     * Returns the table key(AFI/SAFI) corresponding to the Route Entry.
     *
     * @return TablesKey
     */
    @Nonnull
    TablesKey getLocalTablesKey();


    /**
     * Returns the loc-rib table to be updated and to which  corresponds this Route Entry.
     *
     * @return YangInstanceIdentifier containing the path to loc-rib table.
     */
    @Nonnull
    YangInstanceIdentifier getLocRibTableTarget();

    /**
     * Return the ExportPolicyPeerTracker which tracks peers.
     *
     * @return ExportPolicyPeerTracker
     */
    @Nonnull
    ExportPolicyPeerTracker getExportPolicyPeerTracker();
}
