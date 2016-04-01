/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv4;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.labeled.unicast.LUNlriParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.vpn.ipv4._case.DestinationVpnIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.vpn.ipv4.destination.VpnIpv4Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.vpn.ipv4.destination.VpnIpv4DestinationBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class VpnIpv4NlriParser implements NlriParser, NlriSerializer {

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a Attributes object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if ((routes != null) && (routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4Case)) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4Case labeledUnicastCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4Case) routes.getDestinationType();
                serializeNlri(labeledUnicastCase.getDestinationVpnIpv4().getVpnIpv4Destination(), byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if ((mpUnreachNlri.getWithdrawnRoutes() != null) && (mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationVpnIpv4Case)) {
                final DestinationVpnIpv4Case labeledUnicastCase = (DestinationVpnIpv4Case) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(labeledUnicastCase.getDestinationVpnIpv4().getVpnIpv4Destination(), byteAggregator);
            }
        }
    }

    protected static void serializeNlri(final List<VpnIpv4Destination> dests, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final VpnIpv4Destination dest: dests) {
            final List<LabelStack> labelStack = dest.getLabelStack();
            final IpPrefix prefix = dest.getPrefix();
            // Serialize the length field
            // Length field contains one Byte which represents the length of label stack and prefix in bits
            nlriByteBuf.writeByte(((LUNlriParser.LABEL_LENGTH * labelStack.size()) + LUNlriParser.getPrefixLength(prefix) + RouteDistinguisherUtil.RD_LENGTH) * Byte.SIZE);

            LUNlriParser.serializeLabelStackEntries(labelStack, nlriByteBuf);
            RouteDistinguisherUtil.serializeRouteDistinquisher(dest.getRouteDistinguisher(), nlriByteBuf);
            Preconditions.checkNotNull(prefix.getIpv4Prefix(), "Ipv4 prefix is missing.");
            LUNlriParser.serializePrefixField(prefix, nlriByteBuf);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<VpnIpv4Destination> dst = parseNlri(nlri, builder.getAfi());

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4CaseBuilder().setDestinationVpnIpv4(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.vpn.ipv4._case.DestinationVpnIpv4Builder().setVpnIpv4Destination(
                    dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<VpnIpv4Destination> dst = parseNlri(nlri, builder.getAfi());

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationVpnIpv4CaseBuilder().setDestinationVpnIpv4(
                new DestinationVpnIpv4Builder().setVpnIpv4Destination(
                    dst).build()).build()).build());
    }

    private static List<VpnIpv4Destination> parseNlri(final ByteBuf nlri, final Class<? extends AddressFamily> afi) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<VpnIpv4Destination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final VpnIpv4DestinationBuilder builder = new VpnIpv4DestinationBuilder();
            final short length = nlri.readUnsignedByte();
            builder.setLabelStack(LUNlriParser.parseLabel(nlri));
            final int labelNum = builder.getLabelStack().size();
            final int prefixLen = length - (LUNlriParser.LABEL_LENGTH * Byte.SIZE * labelNum) - (RouteDistinguisherUtil.RD_LENGTH * Byte.SIZE);
            builder.setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(nlri));
            Preconditions.checkState(prefixLen > 0, "VPN IPv4 is required.");
            builder.setPrefix(LUNlriParser.parseIpPrefix(nlri, prefixLen, afi));
            dests.add(builder.build());
        }
        return dests;
    }

}
