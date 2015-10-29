/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.labeled.unicast;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.protocol.util.MplsLabelUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class LUNlriParser implements NlriParser, NlriSerializer {

    private static final int LABEL_LENGTH = 3;

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a Attributes object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase labeledUnicastCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase) routes.getDestinationType();
                serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(), byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationLabeledUnicastCase) {
                final DestinationLabeledUnicastCase labeledUnicastCase = (DestinationLabeledUnicastCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(), byteAggregator);
            }
        }
    }

    protected static void serializeNlri(final List<CLabeledUnicastDestination> dests, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final CLabeledUnicastDestination dest: dests) {
            final List<LabelStack> labelStack = dest.getLabelStack();
            final IpPrefix prefix = dest.getPrefix();
            final int stackSize = labelStack.size();
            // Serialize the length field
            // Length field contains one Byte which represents the length of label stack and prefix in bits
            nlriByteBuf.writeByte((LABEL_LENGTH * stackSize + getPrefixLength(prefix)) * Byte.SIZE);

            // Serialize the label stack entries
            int i = 1;
            for (final LabelStack labelStackEntry : labelStack) {
                if (i++ == stackSize) {
                    //mark last label stack entry with bottom-bit
                    nlriByteBuf.writeBytes(MplsLabelUtil.byteBufForMplsLabelWithBottomBit(labelStackEntry.getLabelValue()));
                } else {
                    nlriByteBuf.writeBytes(MplsLabelUtil.byteBufForMplsLabel(labelStackEntry.getLabelValue()));
                }
            }

            // Serialize the prefix field
            final byte[] prefixBytes = getPrefixBytes(prefix);
            nlriByteBuf.writeBytes(Arrays.copyOfRange(prefixBytes, 1, prefixBytes.length));
        }
        buffer.writeBytes(nlriByteBuf);
    }

    private static int getPrefixLength(final IpPrefix prefix) {
        if (prefix.getIpv4Prefix() != null) {
            return Ipv4Util.getPrefixLengthBytes(prefix.getIpv4Prefix().getValue());
        }
        return Ipv4Util.getPrefixLengthBytes(prefix.getIpv6Prefix().getValue());
    }

    private static byte[] getPrefixBytes(final IpPrefix prefix) {
        if (prefix.getIpv4Prefix() != null) {
            return Ipv4Util.bytesForPrefixBegin(prefix.getIpv4Prefix());
        }
        return Ipv6Util.bytesForPrefixBegin(prefix.getIpv6Prefix());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder)
            throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, builder.getAfi());

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(
                    dst).build()).build()).build());
    }

    private static List<CLabeledUnicastDestination> parseNlri(final ByteBuf nlri, final Class<? extends AddressFamily> afi) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<CLabeledUnicastDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
            final short length = nlri.readUnsignedByte();

            builder.setLabelStack(parseLabel(nlri));

            final int labelNum = builder.getLabelStack().size();
            final int prefixLen = length - LABEL_LENGTH * Byte.SIZE * labelNum;
            final int prefixLenInByte = prefixLen / Byte.SIZE + ((prefixLen % Byte.SIZE == 0) ? 0 : 1);
            if (afi.equals(Ipv4AddressFamily.class)) {
                builder.setPrefix(new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(nlri, prefixLenInByte), prefixLen)));
            } else if (afi.equals(Ipv6AddressFamily.class)) {
                builder.setPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(nlri, prefixLenInByte), prefixLen)));
            }
            dests.add(builder.build());
        }
        return dests;
    }

    private static List<LabelStack> parseLabel(final ByteBuf nlri) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<LabelStack> labels = new ArrayList<>();
        long bottomBit = 0;
        do {
            final ByteBuf slice = nlri.readSlice(LABEL_LENGTH);
            bottomBit = MplsLabelUtil.getBottomBit(slice.copy());
            final LabelStackBuilder labelStack = new LabelStackBuilder();
            labelStack.setLabelValue(MplsLabelUtil.mplsLabelForByteBuf(slice));
            labels.add(labelStack.build());
        } while (bottomBit != 1);
        return labels;
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder)
            throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, builder.getAfi());

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(
                    dst).build()).build()).build());
    }
}
