/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * Preexistent routes to be advertized before process any route advertized by the peer.
 *
 * @author Claudio D. Gasparini
 */
@NonNullByDefault
public abstract sealed class AbstractAdvertizedRoute implements RouteKeyIdentifier
        permits ActualBestPathRoutes, AdvertizedRoute {
    private final NodeIdentifierWithPredicates addPathRouteKeyIdentifier;
    private final NodeIdentifierWithPredicates nonAddPathRouteKeyIdentifier;
    private final ContainerNode attributes;
    private final MapEntryNode route;
    private final PeerId fromPeerId;
    private final boolean depreferenced;

    AbstractAdvertizedRoute(final RIBSupport ribSupport, final MapEntryNode route, final BestPath bestPath) {
        this.route = requireNonNull(route);
        addPathRouteKeyIdentifier = ribSupport.toAddPathListArgument(route.name());
        nonAddPathRouteKeyIdentifier = ribSupport.toNonPathListArgument(addPathRouteKeyIdentifier);
        // do not retaim reference
        fromPeerId = bestPath.peerId();
        attributes = bestPath.attributes();
        depreferenced = bestPath.depreferenced();
    }

    public final PeerId getFromPeerId() {
        return fromPeerId;
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
        return route.name();
    }
}
