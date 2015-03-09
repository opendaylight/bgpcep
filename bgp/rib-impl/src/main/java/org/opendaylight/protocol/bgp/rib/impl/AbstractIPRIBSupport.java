/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.config.yang.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common {@link org.opendaylight.protocol.bgp.rib.spi.RIBSupport} class for IPv4 and IPv6 addresses.
 */
abstract class AbstractIPRIBSupport extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIPRIBSupport.class);
    private static final NodeIdentifier ROUTES = new NodeIdentifier(Routes.QNAME);

    protected AbstractIPRIBSupport() {

    }

    protected abstract NodeIdentifier routeIdentifier();
    protected abstract NodeIdentifier routesIdentifier();

    @Override
    public final Collection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return Collections.emptySet();
    }

    @Override
    public final Collection<Class<? extends DataObject>> cacheableNlriObjects() {
        return Collections.emptySet();
    }

    @Override
    public final YangInstanceIdentifier routePath(final YangInstanceIdentifier routesPath, final PathArgument routeId) {
        return routesPath.node(routesIdentifier()).node(routeId);
    }

    @Override
    public final Collection<DataTreeCandidateNode> changedRoutes(final DataTreeCandidateNode routes) {
        final DataTreeCandidateNode myRoutes = routes.getModifiedChild(routesIdentifier());
        if (myRoutes == null) {
            return Collections.emptySet();
        }

        // Well, given the remote possibility of augmentation, we should perform a filter here,
        // to make sure the type matches what routeType() reports.
        return myRoutes.getChildNodes();
    }

    private static enum ApplyRoute {
        DELETE() {
            @Override
            void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final MapEntryNode route, final ContainerNode attributes) {
                // FIXME: we need convert the namespace here, as per the comment below
                tx.delete(LogicalDatastoreType.OPERATIONAL, base.node(route.getIdentifier()));
            }
        },
        PUT() {
            @Override
            void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base, final MapEntryNode route, final ContainerNode attributes) {
                /*
                 * FIXME: We have a problem with namespaces, as the namespace defining the content
                 *        in the message and the namespace defining the content in rib differ.
                 *        Moving ipv4/ipv6 routes out into a separate model would solve the problem,
                 *        but that's going to break our REST compatibility. Is there a generic
                 *        solution to this problem?
                 *
                 *        Even if there is not, we need to transition to that separate model, so
                 *        we get uniform and fast translation.
                 */
                // FIXME: use attributes
                tx.put(LogicalDatastoreType.OPERATIONAL, base.node(route.getIdentifier()), route);
            }
        };

        abstract void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, MapEntryNode route, final ContainerNode attributes);
    }

    private final void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId,
            final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(routeIdentifier());
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof MapNode) {
                    final YangInstanceIdentifier base = tableId.node(ROUTES).node(routesIdentifier());
                    for (MapEntryNode e : ((MapNode)routes).getValue()) {
                        function.apply(tx, base, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    @Override
    protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId, final ContainerNode destination, final ContainerNode attributes) {
        processDestination(tx, tableId, destination, attributes, ApplyRoute.PUT);
    }

    @Override
    protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId, final ContainerNode destination) {
        processDestination(tx, tableId, destination, null, ApplyRoute.DELETE);
    }
}
