/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

/**
 * Routes to be removed from Data Store.
 *
 * @author Claudio D. Gasparini
 */
public abstract class StaleBestPathRoute {
    private final NodeIdentifierWithPredicates nonAddPathRouteKeyIdentifier;

    protected StaleBestPathRoute(final NodeIdentifierWithPredicates nonAddPathRouteKeyIdentifier) {
        this.nonAddPathRouteKeyIdentifier = requireNonNull(nonAddPathRouteKeyIdentifier);
    }

    public final NodeIdentifierWithPredicates getNonAddPathRouteKeyIdentifier() {
        return nonAddPathRouteKeyIdentifier;
    }

    /**
     * Route Identifier List of withdrawn routes to advertize peers supporting additional Path.
     *
     * @return Route Identifier List
     */
    public abstract List<NodeIdentifierWithPredicates> getStaleRouteKeyIdentifiers();

    /**
     * Route Identifier List of withdrawn routes to advertize peers supporting additional Path.
     *
     * @return Route Identifier List
     */
    public abstract List<NodeIdentifierWithPredicates> getAddPathRouteKeyIdentifiers();

    /**
     * Route Identifier of withdrawn routes to advertize peers no supporting additional Path.
     *
     * @return Route Identifier
     */
    public abstract boolean isNonAddPathBestPathNew();
}
