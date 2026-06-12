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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.ChoiceIn;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * Preexistent routes to be advertized before process any route advertized by the peer.
 *
 * @author Claudio D. Gasparini
 */
public abstract class AbstractAdvertizedRoute<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>> implements RouteKeyIdentifier {
    private record CachedEntry(ContainerNode attributes, MapEntryNode entry) {}

    private final PeerId fromPeerId;
    private final MapEntryNode route;
    private final ContainerNode attributes;
    private final NodeIdentifierWithPredicates addPathRouteKeyIdentifier;
    private final NodeIdentifierWithPredicates nonAddPathRouteKeyIdentifier;
    private final boolean depreferenced;

    // Cached AdjRibsOut entries, one per route key.
    private volatile CachedEntry cachedAddPathEntry;
    private volatile CachedEntry cachedNonAddPathEntry;

    // Note: this field hides in the alignment shadow of 'depreferenced', but is used only in AdvertizedRoute.
    // TODO: move this field back when we require JDK15+ (see https://bugs.openjdk.java.net/browse/JDK-8237767)
    final boolean isFirstBestPath;

    AbstractAdvertizedRoute(final RIBSupport<C, S> ribSupport, final MapEntryNode route, final PeerId fromPeerId,
            final ContainerNode attributes, final boolean depreferenced) {
        this(ribSupport, route, fromPeerId, attributes, depreferenced, false);
    }

    AbstractAdvertizedRoute(final RIBSupport<C, S> ribSupport, final MapEntryNode route, final PeerId fromPeerId,
            final ContainerNode attributes, final boolean depreferenced, final boolean isFirstBestPath) {
        this.fromPeerId = fromPeerId;
        this.route = route;
        this.attributes = attributes;
        this.depreferenced = depreferenced;
        this.isFirstBestPath = isFirstBestPath;
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

    /**
     * Return the AdjRibsOut entry for this route with the given effective attributes. The export policy rarely
     * changes attributes per peer, so the entry is usually identical for all peers an update is fanned out to.
     * Build it once and reuse it for equal attributes, so all peers' AdjRibsOut tables share the same node
     * instances instead of holding per-peer copies, which would multiply heap usage by the number of peers.
     *
     * @param ribSupport RIB support instance.
     * @param key route entry key, either {@link #getAddPathRouteKeyIdentifier()} or
     *            {@link #getNonAddPathRouteKeyIdentifier()}.
     * @param effectiveAttributes effective attributes as returned by export policy.
     * @return MapEntryNode to store in the peer's AdjRibsOut table.
     */
    public final MapEntryNode sharedRouteEntry(@NonNull final RIBSupport<C, S> ribSupport,
            @NonNull final NodeIdentifierWithPredicates key, @NonNull final ContainerNode effectiveAttributes) {
        if (key.equals(getAddPathRouteKeyIdentifier())) {
            final var existing = cachedAddPathEntry;
            if (existing != null && existing.attributes.equals(effectiveAttributes)) {
                return existing.entry;
            }
            final MapEntryNode entry = ribSupport.createRoute(route, key, effectiveAttributes);
            cachedAddPathEntry = new CachedEntry(effectiveAttributes, entry);
            return entry;
        }
        if (key.equals(nonAddPathRouteKeyIdentifier)) {
            final var existing = cachedNonAddPathEntry;
            if (existing != null && existing.attributes.equals(effectiveAttributes)) {
                return existing.entry;
            }
            final var entry = ribSupport.createRoute(route, key, effectiveAttributes);
            cachedNonAddPathEntry = new CachedEntry(effectiveAttributes, entry);
            return entry;
        }
        // Unrecognized key: do not cache.
        return ribSupport.createRoute(route, key, effectiveAttributes);
    }
}
