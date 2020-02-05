/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.LabeledUnicastRoutesList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractLabeledUnicastRIBSupport<
        C extends Routes & DataObject,
        S extends ChildOf<? super C> & LabeledUnicastRoutesList>
        extends AbstractRIBSupport<C, S, LabeledUnicastRoute, LabeledUnicastRouteKey> {
    private static final NodeIdentifier LABEL_STACK_NID
            = NodeIdentifier.create(QName.create(CLabeledUnicastDestination.QNAME, "label-stack").intern());
    private static final NodeIdentifier LV_NID
            = NodeIdentifier.create(QName.create(CLabeledUnicastDestination.QNAME, "label-value").intern());
    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(CLabeledUnicastDestination.QNAME);
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLabeledUnicastRIBSupport.class);

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     * @param mappingService  Binding Normalized Node Serializer
     * @param cazeClass Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass Binding class of the container in routes choice, must not be null.
     * @param addressFamilyClass address Family Class
     * @param destinationQname destination Qname
     */
    AbstractLabeledUnicastRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final Class<? extends AddressFamily> addressFamilyClass,
            final QName destinationQname) {
        super(mappingService,
                cazeClass,
                containerClass,
                LabeledUnicastRoute.class,
                addressFamilyClass,
                LabeledUnicastSubsequentAddressFamily.class,
                destinationQname);
    }

    @Override
    protected Collection<NodeIdentifierWithPredicates> processDestination(final DOMDataTreeWriteTransaction tx,
                                                                          final YangInstanceIdentifier routesPath,
                                                                          final ContainerNode destination,
                                                                          final ContainerNode attributes,
                                                                          final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes
                    = destination.getChild(NLRI_ROUTES_LIST);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                    final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).getValue();
                    final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                    for (final UnkeyedListEntryNode labeledUcastDest : routesList) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(labeledUcastDest);
                        function.apply(tx, base, routeKey, labeledUcastDest, attributes);
                        keys.add(routeKey);
                    }
                    return keys;
                }
                LOG.warn("Routes {} are not a map", routes);
            }
        }
        return Collections.emptyList();
    }


    protected List<CLabeledUnicastDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractCLabeledUnicastDestination).collect(Collectors.toList());
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode labeledUnicast) {
        final ByteBuf buffer = Unpooled.buffer();

        final CLabeledUnicastDestination dest = extractCLabeledUnicastDestination(labeledUnicast);
        LUNlriParser.serializeNlri(Collections.singletonList(dest), false, buffer);
        final String routeKeyValue = ByteArray.encodeBase64(buffer);
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf
                = labeledUnicast.getChild(routePathIdNid());
        return PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(), routeKeyValue, maybePathIdLeaf);
    }

    /**
     * Conversion from DataContainer to LabeledUnicastDestination Object.
     *
     * @param route DataContainer
     * @return LabeledUnicastDestination Object
     */
    private CLabeledUnicastDestination extractCLabeledUnicastDestination(
            final DataContainerNode<? extends PathArgument> route) {
        final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
        builder.setPrefix(extractPrefix(route, prefixNid()));
        builder.setLabelStack(extractLabel(route, LABEL_STACK_NID, LV_NID));
        builder.setPathId(PathIdUtil.buildPathId(route, routePathIdNid()));
        return builder.build();
    }

    protected abstract IpPrefix extractPrefix(DataContainerNode<? extends PathArgument> route,
            NodeIdentifier prefixTypeNid);

    public static List<LabelStack> extractLabel(final DataContainerNode<? extends PathArgument> route,
            final NodeIdentifier labelStackNid, final NodeIdentifier labelValueNid) {
        final List<LabelStack> labels = new ArrayList<>();
        final Optional<DataContainerChild<? extends PathArgument, ?>> labelStacks = route.getChild(labelStackNid);
        if (labelStacks.isPresent()) {
            for (final UnkeyedListEntryNode label : ((UnkeyedListNode) labelStacks.get()).getValue()) {
                final Optional<DataContainerChild<? extends PathArgument, ?>> labelStack
                        = label.getChild(labelValueNid);
                if (labelStack.isPresent()) {
                    final LabelStackBuilder labelStackbuilder = new LabelStackBuilder();
                    labelStackbuilder.setLabelValue(new MplsLabel((Uint32) labelStack.get().getValue()));
                    labels.add(labelStackbuilder.build());
                }
            }
        }
        return labels;
    }

    @Override
    public final LabeledUnicastRouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new LabeledUnicastRouteKey(pathId, routeKey);
    }

    @Override
    public final LabeledUnicastRoute createRoute(final LabeledUnicastRoute route, final LabeledUnicastRouteKey key,
            final Attributes attributes) {
        final LabeledUnicastRouteBuilder builder;
        if (route != null) {
            builder = new LabeledUnicastRouteBuilder(route);
        } else {
            builder = new LabeledUnicastRouteBuilder();
        }
        return builder.withKey(key).setAttributes(attributes).build();
    }

    @Override
    public final PathId extractPathId(final LabeledUnicastRouteKey routeListKey) {
        return routeListKey.getPathId();
    }

    @Override
    public final String extractRouteKey(final LabeledUnicastRouteKey routeListKey) {
        return routeListKey.getRouteKey();
    }
}
