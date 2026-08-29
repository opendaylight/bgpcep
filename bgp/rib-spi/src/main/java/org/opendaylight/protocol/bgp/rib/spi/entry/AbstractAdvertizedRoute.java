/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * Preexistent routes to be advertized before process any route advertized by the peer.
 *
 * @author Claudio D. Gasparini
 */
public abstract class AbstractAdvertizedRoute<C extends Routes & DataObject, S extends ChildOf<? super C>>
        implements RouteKeyIdentifier {
    private final PeerId fromPeerId;
    private final MapEntryNode route;
    private final ContainerNode attributes;
    private final @NonNull NodeIdentifierWithPredicates addPathRouteKeyIdentifier;
    private final @NonNull NodeIdentifierWithPredicates nonAddPathRouteKeyIdentifier;
    private final boolean depreferenced;

    AbstractAdvertizedRoute(final RIBSupport<C, S> ribSupport, final MapEntryNode route, final PeerId fromPeerId,
            final ContainerNode attributes, final boolean depreferenced) {
        this.fromPeerId = fromPeerId;
        this.route = route;
        this.attributes = attributes;
        this.depreferenced = depreferenced;
        addPathRouteKeyIdentifier = ribSupport.toAddPathListArgument(route.name());
        nonAddPathRouteKeyIdentifier = ribSupport.toNonPathListArgument(addPathRouteKeyIdentifier);
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
