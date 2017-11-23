/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import java.util.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.MultiPathAbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common {@link org.opendaylight.protocol.bgp.rib.spi.RIBSupport} class for IPv4 and IPv6 addresses.
 */
abstract class AbstractIPRIBSupport extends MultiPathAbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIPRIBSupport.class);
    private final NodeIdentifier prefixNid;
    private final NodeIdentifier nlriRoutesList;
    private final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects;

    protected AbstractIPRIBSupport(final Class<? extends DataObject> prefixClass, final Class<? extends AddressFamily> addressFamilyClass,
        final Class<? extends Routes> cazeClass, final Class<? extends DataObject> containerClass, final Class<? extends Route> listClass,
        final QName destinationQname, final QName prefixesQname) {
        super(cazeClass, containerClass, listClass, addressFamilyClass, UnicastSubsequentAddressFamily.class, "prefix", destinationQname);
        this.prefixNid = new NodeIdentifier(routeKeyQName());
        this.nlriRoutesList = new NodeIdentifier(prefixesQname);
        this.cacheableNlriObjects = ImmutableSet.of(prefixClass);
    }

    protected final NodeIdentifier routePrefixIdentifier() {
        return this.prefixNid;
    }

    @Nonnull
    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Nonnull
    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return this.cacheableNlriObjects;
    }

    @Override
    public final boolean isComplexRoute() {
        return false;
    }

    @Override
    protected void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(this.nlriRoutesList);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    // Instance identifier to table/(choice routes)/(map of route)
                    // FIXME: cache on per-table basis (in TableContext, for example)
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeNid());
                    for (final UnkeyedListEntryNode e : ((UnkeyedListNode) routes).getValue()) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                        function.apply(tx, base, routeKey, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    /**
     * Prefix and Path Id are the route key
     *
     * @param prefixes UnkeyedListEntryNode containing route
     * @return Nid with Route Key
     */
    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode prefixes) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePrefixLeaf = prefixes.getChild(routePrefixIdentifier());
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf = prefixes.getChild(routePathIdNid());
        Preconditions.checkState(maybePrefixLeaf.isPresent());
        final Object prefixValue = (maybePrefixLeaf.get()).getValue();
        return PathIdUtil.createNidKey(routeQName(), routeKeyQName(), pathIdQName(), prefixValue, maybePathIdLeaf);
    }
}
