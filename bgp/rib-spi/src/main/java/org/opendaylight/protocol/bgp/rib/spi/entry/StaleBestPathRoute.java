/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.entry;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID_VALUE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
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
public final class StaleBestPathRoute<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>,
        I extends Identifier<R>> {
    private final I nonAddPathRouteKeyIdentifier;
    private final List<I> addPathRouteKeyIdentifier;
    private final boolean isNonAddPathBestPathNew;
    private final List<I> staleRouteKeyIdentifier;

    public StaleBestPathRoute(
            final RIBSupport<C, S, R, I> ribSupport,
            final String routeKey,
            final List<Long> staleRoutesPathIds,
            final List<Long> withdrawalRoutePathIds,
            final boolean isNonAddPathBestPathNew) {
        this.isNonAddPathBestPathNew = isNonAddPathBestPathNew;

        this.staleRouteKeyIdentifier = staleRoutesPathIds.stream().map(StaleBestPathRoute::pathIdObj)
                .map(pathId -> ribSupport.createRouteListKey(pathId, routeKey)).collect(Collectors.toList());
        if (withdrawalRoutePathIds != null) {
            this.addPathRouteKeyIdentifier = withdrawalRoutePathIds.stream().map(StaleBestPathRoute::pathIdObj)
                    .map(pathId -> ribSupport.createRouteListKey(pathId, routeKey)).collect(Collectors.toList());
        } else {
            this.addPathRouteKeyIdentifier = Collections.emptyList();
        }
        this.nonAddPathRouteKeyIdentifier = ribSupport.createRouteListKey(routeKey);
    }

    public StaleBestPathRoute(final RIBSupport<C, S, R, I> ribSupport, final String routeKey) {
        this(ribSupport, routeKey, Collections.singletonList(NON_PATH_ID_VALUE),
                Collections.emptyList(), true);
    }

    public I getNonAddPathRouteKeyIdentifier() {
        return this.nonAddPathRouteKeyIdentifier;
    }

    /**
     * Route Identifier List of withdrawn routes to advertize peers supporting additional Path.
     *
     * @return Route Identifier List
     */
    public List<I> getStaleRouteKeyIdentifiers() {
        return this.staleRouteKeyIdentifier;
    }

    /**
     * Route Identifier List of withdrawn routes to advertize peers supporting additional Path.
     *
     * @return Route Identifier List
     */
    public List<I> getAddPathRouteKeyIdentifiers() {
        return this.addPathRouteKeyIdentifier;
    }

    /**
     * Route Identifier of withdrawn routes to advertize peers no supporting additional Path.
     *
     * @return Route Identifier
     */
    public boolean isNonAddPathBestPathNew() {
        return this.isNonAddPathBestPathNew;
    }

    private static PathId pathIdObj(final Long pathId) {
        return pathId == NON_PATH_ID_VALUE ? PathIdUtil.NON_PATH_ID : new PathId(pathId);
    }
}
