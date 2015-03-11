/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public final class LinkstateRIBSupport extends AbstractRIBSupport {

    private static final LinkstateRIBSupport SINGLETON = new LinkstateRIBSupport();
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(LinkstateRoutes.QNAME))
            .withChild(ImmutableNodes.mapNodeBuilder(LinkstateRoute.QNAME).build()).build()).build();
    private final NodeIdentifier advertisedDestination = new NodeIdentifier(DestinationLinkstateCase.QNAME);
    private final NodeIdentifier withdrawnDestination = new NodeIdentifier(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase.QNAME);

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
    protected NodeIdentifier advertisedDestinationContainerIdentifier() {
        return this.advertisedDestination;
    }

    @Override
    protected NodeIdentifier withdrawnDestinationContainerIdentifier() {
        return this.withdrawnDestination;
    }

    @Override
    protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final ContainerNode attributes) {
        // TODO Auto-generated method stub
    }
}
