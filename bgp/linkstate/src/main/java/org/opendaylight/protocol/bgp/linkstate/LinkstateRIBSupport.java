/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import com.google.common.base.Optional;
import java.util.Collection;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
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

public final class LinkstateRIBSupport extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateRIBSupport.class);

    private static final LinkstateRIBSupport SINGLETON = new LinkstateRIBSupport();
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(LinkstateRoutes.QNAME))
            .withChild(ImmutableNodes.mapNodeBuilder(LinkstateRoute.QNAME).build()).build()).build();
    private final NodeIdentifier destination = new NodeIdentifier(LinkstateDestination.QNAME);
    private final NodeIdentifier route = new NodeIdentifier(LinkstateRoute.QNAME);

    protected LinkstateRIBSupport() {
        super(LinkstateRoutes.QNAME);
    }

    static LinkstateRIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return this.emptyRoutes;
    }

    @Override
    public Collection<Class<? extends DataObject>> cacheableAttributeObjects() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends DataObject>> cacheableNlriObjects() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final ContainerNode attributes) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(this.route);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof MapNode) {
                    final YangInstanceIdentifier base = tablePath.node(ROUTES).node(routesContainerIdentifier());
                    for (final MapEntryNode e : ((MapNode)routes).getValue()) {
                        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
                        b.withNodeIdentifier(e.getIdentifier());

                        // FIXME: All route children, there should be a utility somewhere to do this
                        for (final DataContainerChild<? extends PathArgument, ?> child : e.getValue()) {
                            b.withChild(child);
                        }

                        // Add attributes
                        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb = Builders.containerBuilder(attributes);
                        cb.withNodeIdentifier(routeAttributesIdentifier());
                        b.withChild(cb.build());
                        tx.put(LogicalDatastoreType.OPERATIONAL, base.node(e.getIdentifier()), b.build());
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    @Override
    protected NodeIdentifier destinationContainerIdentifier() {
        return this.destination;
    }
}
