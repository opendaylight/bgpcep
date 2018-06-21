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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.protocol.util.MplsLabelUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.destination.CLabeledUnicastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.destination.CLabeledUnicastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class LUNlriParser implements NlriParser, NlriSerializer {

    public static final int LABEL_LENGTH = 3;
    private static final byte[] WITHDRAW_LABEL_BYTE_ARRAY = { (byte) 0x80, (byte) 0x00, (byte) 0x00 };
    private static final int WITHDRAW_LABEL_INT_VALUE = 0x800000;

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a Attributes object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null) {
                final DestinationType destinationType = routes.getDestinationType();
                if ( destinationType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase){
                    final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase labeledUnicastCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCase) routes.getDestinationType();
                    serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(), false, byteAggregator);
                } else if (destinationType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCase) {
                    final  org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCase labeledUnicastCase = ( org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCase) routes.getDestinationType();
                    serializeNlri(labeledUnicastCase.getDestinationIpv6LabeledUnicast().getCLabeledUnicastDestination(), false, byteAggregator);
                }
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if (mpUnreachNlri.getWithdrawnRoutes() != null) {
                final DestinationType destinationType = mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                if (destinationType instanceof DestinationLabeledUnicastCase) {
                    final DestinationLabeledUnicastCase labeledUnicastCase = (DestinationLabeledUnicastCase)
                        mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                    serializeNlri(labeledUnicastCase.getDestinationLabeledUnicast().getCLabeledUnicastDestination(),
                        true, byteAggregator);
                } else if(destinationType instanceof DestinationIpv6LabeledUnicastCase) {
                    final DestinationIpv6LabeledUnicastCase labeledUnicastCase = (DestinationIpv6LabeledUnicastCase)
                        mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
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
            nlriByteBuf.writeByte(((LABEL_LENGTH * (!isUnreachNlri ? labelStack.size() : 1)) + getPrefixLength(prefix)) * Byte.SIZE);

            serializeLabelStackEntries(labelStack, isUnreachNlri, nlriByteBuf);
            serializePrefixField(prefix, nlriByteBuf);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    public static void serializeLabelStackEntries(final List<LabelStack> stack, final boolean isUnreachNlri,
            final ByteBuf buffer) {
        if (!isUnreachNlri) {
            int i = 1;
            for (final LabelStack labelStackEntry : stack) {
                if (i++ == stack.size()) {
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
            ByteBufWriteUtil.writeMinimalPrefix(prefix.getIpv4Prefix(), buffer);
        } else {
            ByteBufWriteUtil.writeMinimalPrefix(prefix.getIpv6Prefix(), buffer);
        }
        return ByteArray.readAllBytes(buffer);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        parseNlri(nlri, builder, null);
    }

    private static List<CLabeledUnicastDestination> parseNlri(final ByteBuf nlri, final Class<? extends AddressFamily> afi, final boolean mPathSupported) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<CLabeledUnicastDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final CLabeledUnicastDestinationBuilder builder = new CLabeledUnicastDestinationBuilder();
            if (mPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final short length = nlri.readUnsignedByte();
            final List<LabelStack> labels = parseLabel(nlri);
            builder.setLabelStack(labels);
            final int labelNum = labels != null ? labels.size() : 1;
            final int prefixLen = length - (LABEL_LENGTH * Byte.SIZE * labelNum);
            builder.setPrefix(parseIpPrefix(nlri, prefixLen, afi));
            dests.add(builder.build());
        }
        return dests;
    }

    public static IpPrefix parseIpPrefix(final ByteBuf nlri, final int prefixLen, final Class<? extends AddressFamily> afi) {
        final int prefixLenInByte = (prefixLen / Byte.SIZE) + (((prefixLen % Byte.SIZE) == 0) ? 0 : 1);
        if (afi.equals(Ipv4AddressFamily.class)) {
            return new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(nlri, prefixLenInByte), prefixLen));
        } else if (afi.equals(Ipv6AddressFamily.class)) {
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

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        parseNlri(nlri, builder, null);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
        final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final Class<? extends AddressFamily> afi = builder.getAfi();
        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
            new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, afi, mPathSupported);

        DestinationType destination = null;
        if(afi == Ipv4AddressFamily.class){
            destination = new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder().setCLabeledUnicastDestination(dst).build()).build();
        } else if(afi == Ipv6AddressFamily.class) {
            destination = new DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new DestinationIpv6LabeledUnicastBuilder()
                .setCLabeledUnicastDestination(dst).build()).build();
        }
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destination).build());
    }

    @Override
    public void parseNlri(@Nonnull final ByteBuf nlri, @Nonnull final MpUnreachNlriBuilder builder, @Nullable final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final Class<? extends AddressFamily> afi = builder.getAfi();

        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint, new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        final List<CLabeledUnicastDestination> dst = parseNlri(nlri, afi, mPathSupported);

        DestinationType destination = null;
        if (afi == Ipv4AddressFamily.class) {
            destination = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp
                .unreach.nlri.withdrawn.routes.destination.type.DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(dst).build()).build();
        } else if (afi == Ipv6AddressFamily.class) {
            destination = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp
                .unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                    .setCLabeledUnicastDestination(dst).build()).build();
        }
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(destination).build());
    }
}
