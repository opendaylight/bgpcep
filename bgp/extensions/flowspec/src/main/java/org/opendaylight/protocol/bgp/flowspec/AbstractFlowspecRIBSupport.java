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
import org.opendaylight.yangtools.yang.binding.Identifiable;
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
        R extends Route & ChildOf<? super S> & Identifiable<?>> extends AbstractRIBSupport<C, S, R> {
    protected final T nlriParser;

    protected AbstractFlowspecRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final Class<R> listClass,
            final AddressFamily afiClass,
            final SubsequentAddressFamily safiClass,
            final QName dstContainerClassQName,
            final T nlriParser
    ) {
        super(mappingService, cazeClass, containerClass, listClass, afiClass, safiClass, dstContainerClassQName);
        this.nlriParser = requireNonNull(nlriParser);
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        return nlriParser.createAdvertizedRoutesDestinationType(
            new Object[] {nlriParser.extractFlowspec(routesCont)},
            pathId
        );
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        return nlriParser.createWithdrawnDestinationType(
            new Object[] {nlriParser.extractFlowspec(Iterables.getOnlyElement(routes))},
            pathId
        );
    }

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

        function.apply(tx, routesYangInstanceIdentifier(routesPath),
            PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(), nlriParser.stringNlri(destination),
                destination.findChildByArg(routePathIdNid())),
            destination, attributes);

        return Collections.singletonList(routeKey);
    }
}
