/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.MultiPathAbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public abstract class AbstractFlowspecRIBSupport<T extends AbstractFlowspecNlriParser> extends MultiPathAbstractRIBSupport {
    protected final T nlriParser;

    protected AbstractFlowspecRIBSupport(
        final Class<? extends Routes> cazeClass,
        final Class<? extends DataObject> containerClass,
        final Class<? extends Route> listClass,
        final Class<? extends AddressFamily> afiClass,
        final Class<? extends SubsequentAddressFamily> safiClass,
        final QName dstContainerClassQName,
        final T nlriParser
    ) {
        super(cazeClass, containerClass, listClass, afiClass, safiClass, "route-key", dstContainerClassQName);

        this.nlriParser = requireNonNull(nlriParser);
    }

    @Override
    @Nonnull
    public final ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Override
    @Nonnull
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    @Override
    public final boolean isComplexRoute() {
        return true;
    }

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        return this.nlriParser.createAdvertizedRoutesDestinationType(
            new Object[] {this.nlriParser.extractFlowspec(routesCont)},
            pathId
        );
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        return this.nlriParser.createWithdrawnDestinationType(
            new Object[] {this.nlriParser.extractFlowspec(Iterables.getOnlyElement(routes))},
            pathId
        );
    }

    @Override
    protected final void processDestination(
        final DOMDataWriteTransaction tx,
        final YangInstanceIdentifier routesPath,
        final ContainerNode destination,
        final ContainerNode attributes,
        final ApplyRoute function
    ) {
        if (destination != null) {
            final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeQName());
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf = destination.getChild(routePathIdNid());
            final String routeKeyValue = this.nlriParser.stringNlri(destination);
            final NodeIdentifierWithPredicates routeKey = PathIdUtil.createNidKey(routeQName(), routeKeyQName(), pathIdQName(), routeKeyValue, maybePathIdLeaf);
            function.apply(tx, base, routeKey, destination, attributes);
        }
    }
}
