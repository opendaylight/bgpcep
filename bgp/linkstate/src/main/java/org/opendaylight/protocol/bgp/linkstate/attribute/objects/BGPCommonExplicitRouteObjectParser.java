/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.linkstate.attribute.objects;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.LspFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.protection.object.CommonProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.protection.object.CommonProtectionObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.protection.object.common.protection.object.LspFlagBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.protection.object.common.protection.object.SegFlagBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.Ipv4PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.Ipv4PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.Ipv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.Ipv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.ProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.ProtectionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.UnnumeredInterfaceCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.UnnumeredInterfaceCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPCommonExplicitRouteObjectParser {
    private static final Logger LOG = LoggerFactory.getLogger(BGPCommonExplicitRouteObjectParser.class);
    protected static final short PROTECTION_SUBOBJECT_TYPE_1 = 1;
    protected static final short PROTECTION_SUBOBJECT_TYPE_2 = 2;
    protected static final int PROTECTION_SUBOBJECT_TYPE1_BODY_LENGTH = 4;
    protected static final int PROTECTION_SUBOBJECT_TYPE2_BODY_LENGTH = 8;
    protected static final int IPV4_BITS_LENGTH = 32;
    protected static final int IPV6_BITS_LENGTH = 128;
    protected static final int IPV4_PREFIX_TYPE = 1;
    protected static final int IPV6_PREFIX_TYPE = 2;
    protected static final int AS_NUMBER_TYPE = 32;
    private static final int PROTECTING = 1;
    private static final int NOTIFICATION = 2;
    private static final int OPERATIONAL = 3;
    private static final int IN_PLACE = 0;
    private static final int REQUIRED = 1;
    private static final int SECONDARY = 0;
    protected static final int LOOSE = 128;
    protected static final int UNNUMERED_INTERFACE = 4;
    protected static final int UNNUMERED_LENGTH = 12;

    private BGPCommonExplicitRouteObjectParser() {
        throw new UnsupportedOperationException();
    }

    public static Subobject parseUnnumeredInterface(final ByteBuf byteBuf, final boolean l) {
        UnnumeredInterfaceCaseBuilder unnum = new UnnumeredInterfaceCaseBuilder();
        unnum.setL(l);
        byteBuf.readByte();
        unnum.setTeRouterId(Ipv4Util.addressForByteBuf(byteBuf));
        unnum.setRouterId(byteBuf.readUnsignedInt());
        return unnum.build();
    }

    protected static Ipv6PrefixCase parseIpv6Prefix(final ByteBuf byteBuf, final boolean l) {
        Ipv6PrefixCaseBuilder ipv6Case = new Ipv6PrefixCaseBuilder();
        ipv6Case.setL(l);
        ipv6Case.setIpv6Address(Ipv6Util.addressForByteBuf(byteBuf));
        return ipv6Case.build();
    }

    protected static Ipv4PrefixCase parseIpv4Prefix(final ByteBuf byteBuf, final boolean l) {
        Ipv4PrefixCaseBuilder ipv4Case = new Ipv4PrefixCaseBuilder();
        ipv4Case.setL(l);
        ipv4Case.setIpv4Address(Ipv4Util.addressForByteBuf(byteBuf));
        return ipv4Case.build();
    }

    protected static AsNumberCase parseASNumber(final ByteBuf byteBuf, final boolean l) {
        AsNumberCaseBuilder as = new AsNumberCaseBuilder();
        as.setL(l);
        as.setAsNumber(new AsNumber(byteBuf.readUnsignedInt()));
        return as.build();
    }

    protected static ProtectionCase parseProtectionSubObject(final ByteBuf byteBuf, final boolean l) {
        ProtectionCaseBuilder builder = new ProtectionCaseBuilder();
        builder.setL(l);
        //skip reserved
        byteBuf.readByte();
        final short cType = byteBuf.readUnsignedByte();
        switch (cType) {
        case PROTECTION_SUBOBJECT_TYPE_1:
            builder.setCommonProtectionObject(parseCommonProtectionBodyType1(byteBuf));
            break;
        case PROTECTION_SUBOBJECT_TYPE_2:
            builder.setCommonProtectionObject(parseCommonProtectionBodyType2(byteBuf));
            break;
        default:
            LOG.warn("Secondary Explicit Route Protection Subobject cType {} not supported", cType);
            break;
        }
        return builder.build();
    }

    public static CommonProtectionObject parseCommonProtectionBodyType1(final ByteBuf byteBuf) {
        BitArray bitArray = BitArray.valueOf(byteBuf.readByte());
        CommonProtectionObjectBuilder commonProtectionBuilder = new CommonProtectionObjectBuilder();
        commonProtectionBuilder.setCType(PROTECTION_SUBOBJECT_TYPE_1);
        commonProtectionBuilder.setSecondary(bitArray.get(SECONDARY));
        //Skip Reserved
        byteBuf.readShort();
        int linkFlags = byteBuf.readByte();
        commonProtectionBuilder.setLinkFlags(CommonProtectionObject.LinkFlags.forValue(linkFlags));
        return commonProtectionBuilder.build();
    }

    public static CommonProtectionObject parseCommonProtectionBodyType2(final ByteBuf byteBuf) {
        CommonProtectionObjectBuilder commonProtectionBuilder = new CommonProtectionObjectBuilder();
        final BitArray bitArray1 = BitArray.valueOf(byteBuf.readByte());

        commonProtectionBuilder.setSecondary(bitArray1.get(SECONDARY));
        commonProtectionBuilder.setProtecting(bitArray1.get(PROTECTING));
        commonProtectionBuilder.setNotification(bitArray1.get(NOTIFICATION));
        commonProtectionBuilder.setOperational(bitArray1.get(OPERATIONAL));

        final int lspFlags = byteBuf.readByte();
        commonProtectionBuilder.setLspFlag(new LspFlagBuilder().setFlag(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.bgp.te.lsp.rev150706.LspFlag.Flag.forValue(lspFlags)).build());
        //Skip Reserved
        byteBuf.readByte();
        final int linkFlags = byteBuf.readByte();
        commonProtectionBuilder.setLinkFlags(CommonProtectionObject.LinkFlags.forValue(linkFlags));

        final BitArray bitArray2 = BitArray.valueOf(byteBuf.readByte());
        commonProtectionBuilder.setInPlace(bitArray2.get(IN_PLACE));
        commonProtectionBuilder.setRequired(bitArray2.get(REQUIRED));

        final int segFlags = byteBuf.readByte();
        commonProtectionBuilder.setSegFlag(new SegFlagBuilder().setFlag(LspFlag.Flag.forValue(segFlags)).build());
        byteBuf.readShort();
        return commonProtectionBuilder.build();
    }

    public static void serializeBodyType1(final CommonProtectionObject commonProtectionObject, final ByteBuf output) {
        final BitArray bitArray = new BitArray(AbstractBGPObjectParser.FLAGS_SIZE);
        bitArray.set(SECONDARY, commonProtectionObject.isSecondary());
        bitArray.toByteBuf(output);
        output.writeZero(AbstractBGPObjectParser.SHORT_SIZE);
        output.writeByte(commonProtectionObject.getLinkFlags().getIntValue());
    }

    public static void serializeBodyType2(final CommonProtectionObject commonProtectionObject, final ByteBuf output) {
        BitArray bitArray = new BitArray(AbstractBGPObjectParser.FLAGS_SIZE);

        bitArray.set(SECONDARY, commonProtectionObject.isSecondary());
        bitArray.set(PROTECTING, commonProtectionObject.isProtecting());
        bitArray.set(NOTIFICATION, commonProtectionObject.isNotification());
        bitArray.set(OPERATIONAL, commonProtectionObject.isOperational());
        bitArray.toByteBuf(output);
        output.writeByte(commonProtectionObject.getLspFlag().getFlag().getIntValue());
        output.writeZero(AbstractBGPObjectParser.BYTE_SIZE);
        output.writeByte(commonProtectionObject.getLinkFlags().getIntValue());
        bitArray = new BitArray(AbstractBGPObjectParser.FLAGS_SIZE);
        bitArray.set(IN_PLACE, commonProtectionObject.isInPlace());
        bitArray.set(REQUIRED, commonProtectionObject.isRequired());
        bitArray.toByteBuf(output);
        output.writeByte(commonProtectionObject.getSegFlag().getFlag().getIntValue());
        output.writeZero(AbstractBGPObjectParser.SHORT_SIZE);
    }



    public static void serializeIPV4Prefix(final Ipv4PrefixCase ipv4Prefix, final ByteBuf output) {
        if (ipv4Prefix.isL()) {
            output.writeByte(IPV4_PREFIX_TYPE + LOOSE);
        } else {
            output.writeByte(IPV4_PREFIX_TYPE);
        }
        output.writeByte(AbstractBGPObjectParser.IPV4_PREFIX_LENGTH);
        output.writeBytes(Ipv4Util.byteBufForAddress(ipv4Prefix.getIpv4Address()));
        output.writeByte(IPV4_BITS_LENGTH);
        output.writeZero(AbstractBGPObjectParser.RESERVED);
    }

    public static void serializeIPV6Prefix(final Ipv6PrefixCase ipv6Prefix, final ByteBuf output) {
        if (ipv6Prefix.isL()) {
            output.writeByte(IPV6_PREFIX_TYPE + LOOSE);
        } else {
            output.writeByte(IPV6_PREFIX_TYPE);
        }
        output.writeByte(AbstractBGPObjectParser.IPV6_PREFIX_LENGTH);
        output.writeBytes(Ipv6Util.byteBufForAddress(ipv6Prefix.getIpv6Address()));
        output.writeByte(IPV6_BITS_LENGTH);
        output.writeZero(AbstractBGPObjectParser.RESERVED);
    }

    public static void serializeASNumber(final AsNumberCase as, final ByteBuf output) {
        if (as.isL()) {
            output.writeByte(AS_NUMBER_TYPE + LOOSE);
        } else {
            output.writeByte(AS_NUMBER_TYPE);
        }
        output.writeByte(AbstractBGPObjectParser.AUTONOMUS_SYSTEM_LENGTH);
    }

    public static void serializeUnnumbered(final UnnumeredInterfaceCase unnum, final ByteBuf bufferAux) {
        if (unnum.isL()) {
            bufferAux.writeByte(UNNUMERED_INTERFACE + LOOSE);
        } else {
            bufferAux.writeByte(UNNUMERED_INTERFACE);
        }
        bufferAux.writeByte(UNNUMERED_LENGTH);
        bufferAux.writeZero(AbstractBGPObjectParser.RESERVED);
        bufferAux.writeBytes(Ipv4Util.byteBufForAddress(unnum.getTeRouterId()));
        bufferAux.writeLong(unnum.getRouterId());
    }
}
