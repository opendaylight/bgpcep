/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.protocol.util.MplsLabelUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;

public class LUNlriParser implements NlriParser, NlriSerializer {

    public static final int LABEL_LENGTH = 3;
    private static final byte[] WITHDRAW_LABEL_BYTE_ARRAY = { (byte) 0x80, (byte) 0x00, (byte) 0x00 };
    private static final int WITHDRAW_LABEL_INT_VALUE = 0x800000;

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final AttributesReach pathAttributes1 = pathAttributes.augmentation(AttributesReach.class);
        final AttributesUnreach pathAttributes2 = pathAttributes.augmentation(AttributesUnreach.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (routes != null) {
                final DestinationType destinationType = routes.getDestinationType();
                if (destinationType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                    .DestinationLabeledUnicastCase labeledUnicastCase) {
                    serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(),
                        false, byteAggregator);
                } else if (destinationType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                        .bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination
                        .type.DestinationIpv6LabeledUnicastCase labeledUnicastCase) {
                    serializeNlri(labeledUnicastCase.getDestinationIpv6LabeledUnicast().getCLabeledUnicastDestination(),
                        false, byteAggregator);
                }
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            final WithdrawnRoutes withDrawnRoutes = mpUnreachNlri.getWithdrawnRoutes();

            if (withDrawnRoutes != null) {
                final DestinationType destinationType = withDrawnRoutes.getDestinationType();
                if (destinationType instanceof DestinationLabeledUnicastCase labeledUnicastCase) {
                    serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(),
                        true, byteAggregator);
                } else if (destinationType instanceof DestinationIpv6LabeledUnicastCase labeledUnicastCase) {
                    serializeNlri(labeledUnicastCase.getDestinationIpv6LabeledUnicast().getCLabeledUnicastDestination(),
                        true, byteAggregator);
                }
            }
        }
    }

    protected static void serializeNlri(final List<CLabeledUnicastDestination> dests, final boolean isUnreachNlri,
            final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final CLabeledUnicastDestination dest : dests) {
            PathIdUtil.writePathId(dest.getPathId(), buffer);

            final List<LabelStack> labelStack = dest.getLabelStack();
            final IpPrefix prefix = dest.getPrefix();
            // Serialize the length field
            // Length field contains one Byte which represents the length of label stack and prefix in bits
            nlriByteBuf.writeByte((LABEL_LENGTH * (!isUnreachNlri ? labelStack.size() : 1)
                    + getPrefixLength(prefix)) * Byte.SIZE);

            serializeLabelStackEntries(labelStack, isUnreachNlri, nlriByteBuf);
            serializePrefixField(prefix, nlriByteBuf);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    public static void serializeLabelStackEntries(final List<LabelStack> stack, final boolean isUnreachNlri,
            final ByteBuf buffer) {
        if (!isUnreachNlri) {
            int entry = 1;
            for (final LabelStack labelStackEntry : stack) {
                if (entry++ == stack.size()) {
                    // mark last label stack entry with bottom-bit
                    buffer.writeBytes(MplsLabelUtil.byteBufForMplsLabelWithBottomBit(labelStackEntry.getLabelValue()));
                } else {
                    buffer.writeBytes(MplsLabelUtil.byteBufForMplsLabel(labelStackEntry.getLabelValue()));
                }
            }
        } else {
            buffer.writeBytes(WITHDRAW_LABEL_BYTE_ARRAY);
        }
    }

    public static void serializePrefixField(final IpPrefix prefix, final ByteBuf buffer) {
        final byte[] prefixBytes = getPrefixBytes(prefix);
        buffer.writeBytes(Arrays.copyOfRange(prefixBytes, 1, prefixBytes.length));
    }

    public static int getPrefixLength(final IpPrefix prefix) {
        if (prefix.getIpv4Prefix() != null) {
            return Ipv4Util.getPrefixLengthBytes(prefix.getIpv4Prefix().getValue());
        }
        return Ipv4Util.getPrefixLengthBytes(prefix.getIpv6Prefix().getValue());
    }

    private static byte[] getPrefixBytes(final IpPrefix prefix) {
        final ByteBuf buffer = Unpooled.buffer();

        if (prefix.getIpv4Prefix() != null) {
            Ipv4Util.writeMinimalPrefix(prefix.getIpv4Prefix(), buffer);
        } else {
            Ipv6Util.writeMinimalPrefix(prefix.getIpv6Prefix(), buffer);
        }
        return ByteArray.readAllBytes(buffer);
    }

    public static IpPrefix parseIpPrefix(final ByteBuf nlri, final int prefixLen, final AddressFamily afi) {
        final int prefixLenInByte = prefixLen / Byte.SIZE + (prefixLen % Byte.SIZE == 0 ? 0 : 1);
        if (afi.equals(Ipv4AddressFamily.VALUE)) {
            return new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(nlri, prefixLenInByte), prefixLen));
        } else if (afi.equals(Ipv6AddressFamily.VALUE)) {
            return new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(nlri, prefixLenInByte), prefixLen));
        }
        return null;
    }

    public static List<LabelStack> parseLabel(final ByteBuf nlri) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<LabelStack> labels = new ArrayList<>();
        boolean bottomBit;
        do {
            final ByteBuf slice = nlri.readSlice(LABEL_LENGTH);
            bottomBit = MplsLabelUtil.getBottomBit(slice);
            final MplsLabel mplsLabel = MplsLabelUtil.mplsLabelForByteBuf(slice);
            if (MplsLabelUtil.intForMplsLabel(mplsLabel) == WITHDRAW_LABEL_INT_VALUE) {
                return null;
            }
            labels.add(new LabelStackBuilder().setLabelValue(mplsLabel).build());
        } while (!bottomBit);
        return labels;
    }

    private static List<CLabeledUnicastDestination> parseNlri(final ByteBuf nlri, final AddressFamily afi,
            final boolean multiPathSupported) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<CLabeledUnicastDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
            if (multiPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final short length = nlri.readUnsignedByte();
            final List<LabelStack> labels = parseLabel(nlri);
            builder.setLabelStack(labels);
            final int labelNum = labels != null ? labels.size() : 1;
            final int prefixLen = length - LABEL_LENGTH * Byte.SIZE * labelNum;
            builder.setPrefix(parseIpPrefix(nlri, prefixLen, afi));
            dests.add(builder.build());
        }
        return dests;
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
        final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final AddressFamily afi = builder.getAfi();
        final boolean multiPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
            new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, afi, multiPathSupported);

        DestinationType destination = null;
        if (afi.equals(Ipv4AddressFamily.VALUE)) {
            destination = new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(dst).build()).build();
        } else if (afi.equals(Ipv6AddressFamily.VALUE)) {
            destination = new DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new DestinationIpv6LabeledUnicastBuilder()
                .setCLabeledUnicastDestination(dst).build()).build();
        }
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destination).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final AddressFamily afi = builder.getAfi();

        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
            new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, afi, mPathSupported);

        DestinationType destination = null;
        if (afi.equals(Ipv4AddressFamily.VALUE)) {
            destination = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast
                    .rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast
                        .rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination
                        .labeled.unicast._case.DestinationLabeledUnicastBuilder()
                        .setCLabeledUnicastDestination(dst).build()).build();
        } else if (afi.equals(Ipv6AddressFamily.VALUE)) {
            destination = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast
                    .rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast
                        .rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.ipv6
                        .labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                        .setCLabeledUnicastDestination(dst).build()).build();
        }
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(destination).build());
    }
}
