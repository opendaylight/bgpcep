/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Contains required dependencies to write/update/remove route under DS
 */
public interface RouteEntryDependenciesContainer {
    RIBSupport getRibSupport();

    BGPRIBRoutingPolicy getRoutingPolicies();

    TablesKey getLocalTablesKey();

    YangInstanceIdentifier getLocRibYiID();
}
