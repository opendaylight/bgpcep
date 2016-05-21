/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.MultiPathAbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.labeled.unicast.routes.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
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

public final class LabeledUnicastRIBSupport extends MultiPathAbstractRIBSupport {

    private static final Logger LOG = LoggerFactory.getLogger(LabeledUnicastRIBSupport.class);
    private static final NodeIdentifier PREFIX_TYPE_NID = NodeIdentifier.create(QName.create(CLabeledUnicastDestination.QNAME, "prefix").intern());
    private static final NodeIdentifier LABEL_STACK_NID = NodeIdentifier.create(QName.create(CLabeledUnicastDestination.QNAME, "label-stack").intern());
    private static final NodeIdentifier LV_NID = NodeIdentifier.create(QName.create(CLabeledUnicastDestination.QNAME, "label-value").intern());
    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(CLabeledUnicastDestination.QNAME);

    public LabeledUnicastRIBSupport(final Class<? extends AddressFamily> afiType) {
        super(LabeledUnicastRoutesCase.class, LabeledUnicastRoutes.class, LabeledUnicastRoute.class, afiType, LabeledUnicastSubsequentAddressFamily
            .class, "route-key", DestinationLabeledUnicast.QNAME);
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    @Override
    public boolean isComplexRoute() {
        return true;
    }

    @Override
    protected void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(NLRI_ROUTES_LIST);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeNid());
                    for (final UnkeyedListEntryNode e : ((UnkeyedListNode)routes).getValue()) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                        function.apply(tx, base, routeKey, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode labeledUnicast) {
        final ByteBuf buffer = Unpooled.buffer();

        final CLabeledUnicastDestination dest = extractCLabeledUnicastDestination(labeledUnicast);
        LUNlriParser.serializeNlri(Collections.singletonList(dest), buffer);
        final byte[] routeKeyValue = ByteArray.readAllBytes(buffer);
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf = labeledUnicast.getChild(routePathIdNid());
        final NodeIdentifierWithPredicates routeKey = PathIdUtil.createNidKey(routeQName(), routeKeyQName(), pathIdQName(), routeKeyValue, maybePathIdLeaf);
        return routeKey;
    }

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
            new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(extractRoutes(routes)).build()).build();
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(
                extractRoutes(routes)).build()).build();
    }

    private List<CLabeledUnicastDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractCLabeledUnicastDestination).collect(Collectors.toList());
    }

    /**
     * Conversion from DataContainer to LabeledUnicastDestination Object
     *
     * @param route DataContainer
     * @return LabeledUnicastDestination Object
     */
    private CLabeledUnicastDestination extractCLabeledUnicastDestination(final DataContainerNode<? extends PathArgument> route) {
        final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
        builder.setPrefix(extractPrefix(route, PREFIX_TYPE_NID));
        builder.setLabelStack(extractLabel(route, LABEL_STACK_NID, LV_NID));
        builder.setPathId(PathIdUtil.buildPathId(route, routePathIdNid()));
        return builder.build();
    }

    public static IpPrefix extractPrefix(final DataContainerNode<? extends PathArgument> route, final NodeIdentifier prefixTypeNid) {
        if (route.getChild(prefixTypeNid).isPresent()) {
            final String prefixType = (String) route.getChild(prefixTypeNid).get().getValue();
            try {
                Ipv4Util.bytesForPrefixBegin(new Ipv4Prefix(prefixType));
                return new IpPrefix(new Ipv4Prefix(prefixType));
            } catch (final IllegalArgumentException e) {
                LOG.debug("Creating Ipv6 prefix because", e);
                return new IpPrefix(new Ipv6Prefix(prefixType));
            }
        }
        return null;
    }

    public static List<LabelStack> extractLabel(final DataContainerNode<? extends PathArgument> route, final NodeIdentifier labelStackNid, final NodeIdentifier labelValueNid) {
        final List<LabelStack> labels = new ArrayList<>();
        final Optional<DataContainerChild<? extends PathArgument, ?>> labelStacks = route.getChild(labelStackNid);
        if (labelStacks.isPresent()) {
            for (final UnkeyedListEntryNode label : ((UnkeyedListNode) labelStacks.get()).getValue()) {
                final Optional<DataContainerChild<? extends PathArgument, ?>> labelStack = label.getChild(labelValueNid);
                if (labelStack.isPresent()) {
                    final LabelStackBuilder labelStackbuilder = new LabelStackBuilder();
                    labelStackbuilder.setLabelValue(new MplsLabel((Long) labelStack.get().getValue()));
                    labels.add(labelStackbuilder.build());
                }
            }
        }
        return labels;
    }
}
