/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecRIBSupport;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public abstract class AbstractFlowspecL3vpnRIBSupport<T extends AbstractFlowspecL3vpnNlriParser> extends AbstractFlowspecRIBSupport<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowspecL3vpnRIBSupport.class);

    private final NodeIdentifier RD_NID;

    protected AbstractFlowspecL3vpnRIBSupport(
        final Class<? extends Routes> cazeClass,
        final Class<? extends DataObject> containerClass,
        final Class<? extends Route> listClass,
        final QName dstContainerClassQName,
        final Class<? extends AddressFamily> afiClass,
        final T flowspecNlriParser
    ) {
        super(cazeClass, containerClass, listClass, dstContainerClassQName, afiClass, FlowspecL3vpnSubsequentAddressFamily.class, flowspecNlriParser);
        final QName RD_QNAME = QName.create(LIST_CLASS_QNAME, "route-distinguisher").intern();
        RD_NID = new NodeIdentifier(RD_QNAME);
    }

    @Nullable
    private RouteDistinguisher buildRouteDistinguisher(final DataContainerNode<? extends PathArgument> data) {
        final NormalizedNode<?, ?> rdNode = NormalizedNodes.findNode(data, RD_NID).orNull();
        RouteDistinguisher rd = null;
        if (rdNode != null) {
            rd = RouteDistinguisherUtil.parseRouteDistinguisher(rdNode.getValue());
        }
        return rd;
    }

    @Override
    @Nonnull
    protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(AFI_CLASS);
        mb.setSafi(SAFI_CLASS);
        mb.setCNextHop(hop);

        PathId pathId = null;
        RouteDistinguisher rd = null;
        List<Flowspec> flowspecList = new ArrayList<>();

        if (!routes.isEmpty()) {
            final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
            pathId = PathIdUtil.buildPathId(routesCont, PATH_ID_NID);
            rd = buildRouteDistinguisher(routesCont);
            flowspecList = flowspecNlriParser.extractFlowspec(routesCont);
        } else {
            LOG.debug("Building Unreach routes with empty list!");
        }

        mb.setAdvertizedRoutes(
            new AdvertizedRoutesBuilder()
                .setDestinationType(
                    flowspecNlriParser.createAdvertizedRoutesDestinationType(
                        flowspecList, rd, pathId
                    )
                ).build()
        );
        return mb.build();
    }

    @Override
    @Nonnull
    protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(AFI_CLASS);
        mb.setSafi(SAFI_CLASS);

        PathId pathId = null;
        RouteDistinguisher rd = null;
        List<Flowspec> flowspecList = new ArrayList<>();

        if (!routes.isEmpty()) {
            final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
            pathId = PathIdUtil.buildPathId(routesCont, PATH_ID_NID);
            rd = buildRouteDistinguisher(routesCont);
            flowspecList = flowspecNlriParser.extractFlowspec(routesCont);
        } else {
            LOG.debug("Building Unreach routes with empty list!");
        }

        mb.setWithdrawnRoutes(
            new WithdrawnRoutesBuilder()
                .setDestinationType(
                    flowspecNlriParser.createWithdrawnDestinationType(
                        flowspecList, rd, pathId
                    )
                ).build()
        );
        return mb.build();
    }
}
