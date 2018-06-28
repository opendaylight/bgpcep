/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.entry;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID_VALUE;

import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Preexistent routes to be advertized before process any route advertized by the peer.
 *
 * @author Claudio D. Gasparini
 */
public final class ActualBestPathRoutes<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>,
        I extends Identifier<R>> implements RouteKeyIdentifier<R,I> {
    private final PeerId fromPeerId;
    private final R route;
    private final Attributes attributes;
    private final I nonAddPathRouteKeyIdentifier;
    private final I addPathRouteKeyIdentifier;

    public ActualBestPathRoutes(final RIBSupport<C, S, R, I> ribSupport, final R route, final PeerId fromPeerId,
            final Attributes attributes) {
        this.fromPeerId = fromPeerId;
        this.route = route;
        this.attributes = attributes;
        this.nonAddPathRouteKeyIdentifier
                = ribSupport.createRouteListKey(NON_PATH_ID_VALUE, route.getRouteKey());
        this.addPathRouteKeyIdentifier
                = ribSupport.createRouteListKey(route.getPathId().getValue(), route.getRouteKey());
    }

    public PeerId getFromPeerId() {
        return this.fromPeerId;
    }

    public R getRoute() {
        return route;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public I getNonAddPathRouteKeyIdentifier() {
        return this.nonAddPathRouteKeyIdentifier;
    }

    public I getAddPathRouteKeyIdentifier() {
        return this.addPathRouteKeyIdentifier;
    }
}
