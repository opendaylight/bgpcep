/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnRegistry;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEvpnNlriRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.destination.EvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EsRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EthernetADRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.IncMultiEthernetTagResCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.MacIpAdvRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EvpnNlriParser implements NlriParser, NlriSerializer {
    private static final NodeIdentifier EVPN_TYPE_NID = new NodeIdentifier(Evpn.QNAME);
    private static final Logger LOG = LoggerFactory.getLogger(EvpnNlriParser.class);

    public static EvpnDestination extractCEvpnDestination(final DataContainerNode<? extends PathArgument> evpnDC) {
        final ChoiceNode evpn = (ChoiceNode) evpnDC.getChild(EVPN_TYPE_NID).get();
        final EvpnRegistry reg = SimpleEvpnNlriRegistry.getInstance();
        Evpn evpnValue = null;
        if (evpn.getChild(EthSegRParser.ES_ROUTE_CASE_NID).isPresent()) {
            evpnValue = reg.serializeEvpnModel(EsRouteCase.class, (ChoiceNode) evpn.getChild(EthSegRParser.ES_ROUTE_CASE_NID).get());
        } else if (evpn.getChild(EthADRParser.ETH_AD_ROUTE_CASE_NID).isPresent()) {
            evpnValue = reg.serializeEvpnModel(EthernetADRouteCase.class, (ChoiceNode) evpn.getChild(EthADRParser.ETH_AD_ROUTE_CASE_NID).get());
        } else if (evpn.getChild(IncMultEthTagRParser.INC_MULT_ROUTE_CASE_NID).isPresent()) {
            evpnValue = reg.serializeEvpnModel(IncMultiEthernetTagResCase.class, (ChoiceNode) evpn.getChild(IncMultEthTagRParser.INC_MULT_ROUTE_CASE_NID).get());
        } else if (evpn.getChild(MACIpAdvRParser.MAC_IP_ADV_ROUTE_CASE_NID).isPresent()) {
            evpnValue = reg.serializeEvpnModel(MacIpAdvRouteCase.class, (ChoiceNode) evpn.getChild(MACIpAdvRParser.MAC_IP_ADV_ROUTE_CASE_NID).get());
        } else {
            LOG.warn("Unrecognized Nlri {}", evpn);
        }
        return new EvpnDestinationBuilder().setEvpn(evpnValue).build();
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<EvpnDestination> dst = parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.mp.unreach.nlri.withdrawn.
                routes.destination.type.DestinationEvpnCaseBuilder().setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.
                yang.bgp.evpn.rev160321.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder()
                .setEvpnDestination(dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<EvpnDestination> dst = parseNlri(nlri);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationEvpnCaseBuilder().setDestinationEvpn(new DestinationEvpnBuilder().setEvpnDestination(dst).build()).build()).build());
    }

    private List<EvpnDestination> parseNlri(final ByteBuf nlri) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<EvpnDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final EvpnDestinationBuilder builder = new EvpnDestinationBuilder();
            final NlriType type = NlriType.forValue(nlri.readUnsignedShort());
            final int length = nlri.readUnsignedShort();
            builder.setEvpn(SimpleEvpnNlriRegistry.getInstance().parseEvpn(type, nlri.readSlice(length)));
            dests.add(builder.build());
        }
        return dests;
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a Attributes object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if ((routes != null) && (routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.
                bgp.evpn.rev160321.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCase)) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.mp.reach.nlri.advertized.routes.destination.type.
                    DestinationEvpnCase evpnCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.
                    attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCase) routes.getDestinationType();
                serializeNlri(evpnCase.getDestinationEvpn().getEvpnDestination(), byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if ((mpUnreachNlri.getWithdrawnRoutes() != null) && (mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationEvpnCase)) {
                final DestinationEvpnCase evpnCase = (DestinationEvpnCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(evpnCase.getDestinationEvpn().getEvpnDestination(), byteAggregator);
            }
        }
    }

    public static void serializeNlri(final List<EvpnDestination> cEvpn, final ByteBuf output) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final EvpnDestination dest : cEvpn) {
            SimpleEvpnNlriRegistry.getInstance().serializeEvpn(dest.getEvpn(), nlriByteBuf);
        }
        output.writeBytes(nlriByteBuf);
    }
}
