/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * Preexistent routes to be advertized before process any route advertized by the peer.
 *
 * @author Claudio D. Gasparini
 */
public abstract class AbstractAdvertizedRoute<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>,
        I extends Identifier<R>> implements RouteKeyIdentifier<R,I> {
    private final PeerId fromPeerId;
    private final MapEntryNode route;
    private final ContainerNode attributes;
    private final NodeIdentifierWithPredicates nonAddPathRouteKeyIdentifier;
    private final boolean depreferenced;

    // Note: this field hides in the alignment shadow of 'depreferenced', but is used only in AdvertizedRoute.
    // TODO: move this field back when we require JDK15+ (see https://bugs.openjdk.java.net/browse/JDK-8237767)
    final boolean isFirstBestPath;

    AbstractAdvertizedRoute(final RIBSupport<C, S, R, I> ribSupport, final MapEntryNode route, final PeerId fromPeerId,
            final ContainerNode attributes, final boolean depreferenced) {
        this(ribSupport, route, fromPeerId, attributes, depreferenced, false);
    }

    AbstractAdvertizedRoute(final RIBSupport<C, S, R, I> ribSupport, final MapEntryNode route, final PeerId fromPeerId,
            final ContainerNode attributes, final boolean depreferenced, final boolean isFirstBestPath) {
        this.fromPeerId = fromPeerId;
        this.route = route;
        this.attributes = attributes;
        this.depreferenced = depreferenced;
        this.isFirstBestPath = isFirstBestPath;
        this.nonAddPathRouteKeyIdentifier = ribSupport.toNonPathListArgument(route.getIdentifier());
    }

    public final PeerId getFromPeerId() {
        return this.fromPeerId;
    }

    public final MapEntryNode getRoute() {
        return route;
    }

    public final ContainerNode getAttributes() {
        return attributes;
    }

    public final boolean isDepreferenced() {
        return depreferenced;
    }

    @Override
    public final NodeIdentifierWithPredicates getNonAddPathRouteKeyIdentifier() {
        return nonAddPathRouteKeyIdentifier;
    }

    @Override
    public final NodeIdentifierWithPredicates getAddPathRouteKeyIdentifier() {
        return route.getIdentifier();
    }
}
