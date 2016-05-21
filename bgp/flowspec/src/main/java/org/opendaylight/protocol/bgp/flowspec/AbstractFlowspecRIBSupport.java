/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.MpAbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public abstract class AbstractFlowspecRIBSupport extends MpAbstractRIBSupport {
    private final NodeIdentifier destinationNid;

    protected AbstractFlowspecRIBSupport(final Class<? extends Routes> cazeClass, final Class<? extends DataObject> containerClass,
        final Class<? extends Route> listClass, final QName destinationQname) {
        super(cazeClass, containerClass, listClass, "route-key");
        this.destinationNid = new NodeIdentifier(destinationQname);
    }

    protected abstract AbstractFlowspecNlriParser getParser();

    protected abstract Class<? extends AddressFamily> getAfiClass();

    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    @Override
    public final boolean isComplexRoute() {
        return true;
    }

    @Override
    protected final void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final ContainerNode attributes, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, this.putRoute);
    }

    @Override
    protected final void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    private void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeQName());
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf = destination.getChild(routePathIdNid());
            final String routeKeyValue = getParser().stringNlri(destination);
            final NodeIdentifierWithPredicates routeKey = PathIdUtil.createNidKey(routeQName(), routeKeyQName(), pathIdQName(), routeKeyValue, maybePathIdLeaf);
            function.apply(tx, base, routeKey,  destination, attributes);
        }
    }

    @Override
    protected final MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(getAfiClass());
        mb.setSafi(FlowspecSubsequentAddressFamily.class);
        mb.setCNextHop(hop);

        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());

        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(getParser().createAdvertizedRoutesDestinationType(
            getParser().extractFlowspec(routesCont), pathId)).build());
        return mb.build();
    }

    @Override
    protected final MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(getAfiClass());
        mb.setSafi(FlowspecSubsequentAddressFamily.class);

        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());

        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(getParser().createWithdrawnDestinationType(
            getParser().extractFlowspec(Iterables.getOnlyElement(routes)), pathId)).build());
        return mb.build();
    }

    @Override
    protected  final NodeIdentifier destinationContainerIdentifier() {
        return this.destinationNid;
    }
}
