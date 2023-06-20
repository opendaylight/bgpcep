/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.KeyAware;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

@Beta
public abstract class AbstractFlowspecRIBSupport<
        T extends AbstractFlowspecNlriParser,
        C extends Routes & DataObject,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & KeyAware<?>> extends AbstractRIBSupport<C, S, R> {
    protected final @NonNull T nlriParser;

    protected AbstractFlowspecRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass, final QName cazeQName,
            final Class<S> containerClass, final QName containerQName,
            final Class<R> listClass, final QName listQName,
            final AddressFamily afi, final QName afiQName,
            final SubsequentAddressFamily safi, final QName safiQName,
            final QName dstContainerClassQName,
            final T nlriParser) {
        super(mappingService, cazeClass, cazeQName, containerClass, containerQName, listClass, listQName, afi, afiQName,
            safi, safiQName, dstContainerClassQName);
        this.nlriParser = requireNonNull(nlriParser);
    }

    @Override
    protected final DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        final var route = Iterables.getOnlyElement(routes);
        return buildDestination(route, PathIdUtil.buildPathId(route, routePathIdNid()));
    }

    protected abstract @NonNull DestinationType buildDestination(MapEntryNode route, @Nullable PathId pathId);

    @Override
    protected final DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        final var route = Iterables.getOnlyElement(routes);
        return buildWithdrawnDestination(route, PathIdUtil.buildPathId(route, routePathIdNid()));
    }

    protected abstract @NonNull DestinationType buildWithdrawnDestination(MapEntryNode route, @Nullable PathId pathId);

    @Override
    protected final Collection<NodeIdentifierWithPredicates> processDestination(
        final DOMDataTreeWriteTransaction tx,
        final YangInstanceIdentifier routesPath,
        final ContainerNode destination,
        final ContainerNode attributes,
        final ApplyRoute function
    ) {
        if (destination == null) {
            return Collections.emptyList();
        }

        final var routeKey = PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(),
            nlriParser.stringNlri(destination), destination.findChildByArg(routePathIdNid()));
        function.apply(tx, routesYangInstanceIdentifier(routesPath), routeKey, destination, attributes);
        return Collections.singletonList(routeKey);
    }
}
