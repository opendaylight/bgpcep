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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Routes to be removed from Data Store.
 *
 * @author Claudio D. Gasparini
 */
public abstract class StaleBestPathRoute<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>, R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> {
    private final I nonAddPathRouteKeyIdentifier;

    protected StaleBestPathRoute(final I nonAddPathRouteKeyIdentifier) {
        this.nonAddPathRouteKeyIdentifier = requireNonNull(nonAddPathRouteKeyIdentifier);
    }

    public final I getNonAddPathRouteKeyIdentifier() {
        return nonAddPathRouteKeyIdentifier;
    }

    /**
     * Route Identifier List of withdrawn routes to advertize peers supporting additional Path.
     *
     * @return Route Identifier List
     */
    public abstract List<I> getStaleRouteKeyIdentifiers();

    /**
     * Route Identifier List of withdrawn routes to advertize peers supporting additional Path.
     *
     * @return Route Identifier List
     */
    public abstract List<I> getAddPathRouteKeyIdentifiers();

    /**
     * Route Identifier of withdrawn routes to advertize peers no supporting additional Path.
     *
     * @return Route Identifier
     */
    public abstract boolean isNonAddPathBestPathNew();
}
