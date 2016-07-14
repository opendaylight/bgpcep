/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn;

import com.google.common.collect.Iterables;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecRIBSupport;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * @author Kevin Wang
 */
public abstract class AbstractFlowspecL3vpnRIBSupport<T extends AbstractFlowspecL3vpnNlriParser> extends AbstractFlowspecRIBSupport<T> {
    private final NodeIdentifier routeDistinguisherNID;

    protected AbstractFlowspecL3vpnRIBSupport(
        final Class<? extends Routes> cazeClass,
        final Class<? extends DataObject> containerClass,
        final Class<? extends Route> listClass,
        final QName dstContainerClassQName,
        final Class<? extends AddressFamily> afiClass,
        final T flowspecNlriParser
    ) {
        super(cazeClass, containerClass, listClass, afiClass, FlowspecL3vpnSubsequentAddressFamily.class, dstContainerClassQName, flowspecNlriParser);
        final QName routeDistinguisherQName = QName.create(routeQName(), "route-distinguisher").intern();
        routeDistinguisherNID = new NodeIdentifier(routeDistinguisherQName);
    }

    @Nullable
    private RouteDistinguisher buildRouteDistinguisher(final DataContainerNode<? extends PathArgument> data) {
        final NormalizedNode<?, ?> rdNode = NormalizedNodes.findNode(data, routeDistinguisherNID).orNull();
        RouteDistinguisher rd = null;
        if (rdNode != null) {
            rd = RouteDistinguisherUtil.parseRouteDistinguisher(rdNode.getValue());
        }
        return rd;
    }

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        final RouteDistinguisher rd = buildRouteDistinguisher(routesCont);
        return this.nlriParser.createAdvertizedRoutesDestinationType(
            new Object[] {rd, this.nlriParser.extractFlowspec(routesCont)},
            pathId
        );
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        final RouteDistinguisher rd = buildRouteDistinguisher(routesCont);
        return this.nlriParser.createWithdrawnDestinationType(
            new Object[] {rd, nlriParser.extractFlowspec(Iterables.getOnlyElement(routes))},
            pathId
        );
    }
}
