/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class RouteEntryDependenciesContainerImpl implements RouteEntryDependenciesContainer {
    private final RIBSupport ribSupport;
    private final TablesKey tablesKey;
    private final YangInstanceIdentifier locRibTarget;
    private final ExportPolicyPeerTracker exportPolicyPeerTracker;

    public RouteEntryDependenciesContainerImpl(
            final RIBSupport ribSupport,
            final TablesKey tablesKey,
            final YangInstanceIdentifier locRibTarget, final ExportPolicyPeerTracker exportPolicyPeerTracker) {
        this.ribSupport = requireNonNull(ribSupport);
        this.tablesKey = requireNonNull(tablesKey);
        this.locRibTarget = requireNonNull(locRibTarget);
        this.exportPolicyPeerTracker = requireNonNull(exportPolicyPeerTracker);
    }

    @Override
    public RIBSupport getRibSupport() {
        return this.ribSupport;
    }

    @Override
    public TablesKey getLocalTablesKey() {
        return this.tablesKey;
    }

    @Override
    public YangInstanceIdentifier getLocRibTableTarget() {
        return this.locRibTarget;
    }

    @Override
    public ExportPolicyPeerTracker getExportPolicyPeerTracker() {
        return exportPolicyPeerTracker;
    }
}
