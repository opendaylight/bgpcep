/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common {@link org.opendaylight.protocol.bgp.rib.spi.RIBSupport} class for IPv4 and IPv6 addresses.
 */
abstract class AbstractIPRIBSupport extends AbstractRIBSupport {
    private static abstract class ApplyRoute {
        abstract void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, MapEntryNode route, final ContainerNode attributes);
    }

    private static final class DeleteRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final MapEntryNode route, final ContainerNode attributes) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, base.node(route.getIdentifier()));
        }
    }

    private final class PutRoute extends ApplyRoute {
        @Override
        void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final MapEntryNode route, final ContainerNode attributes) {
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(route.getIdentifier());

            // FIXME: All route children, there should be a utility somewhere to do this
            for (final DataContainerChild<? extends PathArgument, ?> child : route.getValue()) {
                b.withChild(child);
            }

            // Add attributes
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb = Builders.containerBuilder(attributes);
            cb.withNodeIdentifier(routeAttributesIdentifier());
            b.withChild(cb.build());
            tx.put(LogicalDatastoreType.OPERATIONAL, base.node(route.getIdentifier()), b.build());
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIPRIBSupport.class);
    private static final NodeIdentifier ROUTES = new NodeIdentifier(Routes.QNAME);
    private static final ApplyRoute DELETE_ROUTE = new DeleteRoute();
    private final ApplyRoute putRoute = new PutRoute();

    protected AbstractIPRIBSupport(final Class<? extends Routes> cazeClass,
        final Class<? extends DataObject> containerClass, final Class<? extends Route> listClass) {
        super(cazeClass, containerClass, listClass);
    }

    /**
     * Return the NodeIdentifier corresponding to the list containing individual routes.
     *
     * @return The NodeIdentifier for individual route list.
     */
    @Nonnull protected abstract NodeIdentifier routeIdentifier();

    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    private final void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(routeIdentifier());
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof MapNode) {
                    final YangInstanceIdentifier base = tablePath.node(ROUTES).node(routesContainerIdentifier());
                    for (final MapEntryNode e : ((MapNode)routes).getValue()) {
                        function.apply(tx, base, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    @Override
    protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath, final ContainerNode destination, final ContainerNode attributes) {
        processDestination(tx, tablePath, destination, attributes, this.putRoute);
    }

    @Override
    protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath, final ContainerNode destination) {
        processDestination(tx, tablePath, destination, null, DELETE_ROUTE);
    }
}
