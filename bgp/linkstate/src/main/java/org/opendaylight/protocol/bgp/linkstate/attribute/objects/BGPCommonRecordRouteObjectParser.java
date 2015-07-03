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
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.ProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.ProtectionCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPCommonRecordRouteObjectParser {
    private static final Logger LOG = LoggerFactory.getLogger(BGPCommonRecordRouteObjectParser.class);

    protected static final short PROTECTION_SUBOBJECT_TYPE_1 = 1;
    protected static final short PROTECTION_SUBOBJECT_TYPE_2 = 2;
    protected static final int IPV4_PREFIX_TYPE = 1;
    protected static final int IPV6_PREFIX_TYPE = 2;
    protected static final int LABEL_TYPE = 3;
    protected static final int IPV4_CASE_LENGTH = 4;
    protected static final int IPV6_CASE_LENGTH = 16;
    protected static final int LABEL_CASE_LENGTH = 4;
    protected static final int PROTECTION_TYPE2_BODY_SYZE = 8;
    private static final int GLOBAL_LABEL = 8;
    private static final int LOCAL_PROTECTION_AVAILABLE = 8;
    private static final int LOCAL_PROTECTION_IN_USE = 7;
    private static final int IPV4_BITS_LENGTH = 32;
    private static final int IPV6_BITS_LENGTH = 128;

    private BGPCommonRecordRouteObjectParser() {
        throw new UnsupportedOperationException();
    }

    static Subobject parseLabel(final ByteBuf byteBuf) {
        LabelCaseBuilder labelCase = new LabelCaseBuilder();
        final BitArray bsLabel = BitArray.valueOf(byteBuf.readByte());
        labelCase.setFlags(new LabelCase.Flags(bsLabel.get(GLOBAL_LABEL)));
        labelCase.setLabelCType(byteBuf.readUnsignedByte());
        labelCase.setLabel(byteBuf.readLong());
        return labelCase.build();
    }

    protected static Subobject parseIPV6Prefix(final ByteBuf byteBuf) {
        Ipv6CaseBuilder ipv6Case = new Ipv6CaseBuilder().setIpv6Address(Ipv6Util.addressForByteBuf(byteBuf));
        //skip Prefix Length
        byteBuf.readByte();
        final BitArray bsIpv6 = BitArray.valueOf(byteBuf.readByte());
        ipv6Case.setFlags(new Ipv6Case.Flags(bsIpv6.get(LOCAL_PROTECTION_AVAILABLE), bsIpv6.get(LOCAL_PROTECTION_IN_USE)));
        return ipv6Case.build();
    }

    public static Subobject parseIPV4Prefix(final ByteBuf byteBuf) {
        Ipv4CaseBuilder ipv4Case = new Ipv4CaseBuilder().setIpv4Address(Ipv4Util.addressForByteBuf(byteBuf));
        //skip Prefix Length
        byteBuf.readByte();
        final BitArray bsIpv4 = BitArray.valueOf(byteBuf.readByte());
        ipv4Case.setFlags(new Ipv4Case.Flags(bsIpv4.get(LOCAL_PROTECTION_AVAILABLE), bsIpv4.get(LOCAL_PROTECTION_IN_USE)));
        return ipv4Case.build();
    }

    public static void serializeIPV4Prefix(final Ipv4Case ipv4Case, final ByteBuf bufferAux) {
        bufferAux.writeByte(IPV4_PREFIX_TYPE);
        bufferAux.writeByte(IPV4_CASE_LENGTH);
        ByteBufWriteUtil.writeIpv4Address(ipv4Case.getIpv4Address(), bufferAux);
        bufferAux.writeByte(IPV4_BITS_LENGTH);
        final BitArray bs = new BitArray(AbstractBGPObjectParser.FLAGS_SIZE);
        bs.set(LOCAL_PROTECTION_AVAILABLE, ipv4Case.getFlags().isLocalProtectionAvailable());
        bs.set(LOCAL_PROTECTION_IN_USE, ipv4Case.getFlags().isLocalProtectionInUse());
        bs.toByteBuf(bufferAux);
    }

    public static void serializeIPV6Prefix(final Ipv6Case ipv6Case, final ByteBuf bufferAux) {
        bufferAux.writeByte(IPV6_PREFIX_TYPE);
        bufferAux.writeByte(IPV6_CASE_LENGTH);
        ByteBufWriteUtil.writeIpv6Address(ipv6Case.getIpv6Address(), bufferAux);
        bufferAux.writeByte(IPV6_BITS_LENGTH);
        final BitArray bs = new BitArray(AbstractBGPObjectParser.FLAGS_SIZE);
        bs.set(LOCAL_PROTECTION_AVAILABLE, ipv6Case.getFlags().isLocalProtectionAvailable());
        bs.set(LOCAL_PROTECTION_IN_USE, ipv6Case.getFlags().isLocalProtectionInUse());
        bs.toByteBuf(bufferAux);
    }

    public static void serializeLabel(final LabelCase labelCase, final ByteBuf bufferAux) {
        bufferAux.writeByte(LABEL_TYPE);
        bufferAux.writeByte(LABEL_CASE_LENGTH);
        final BitArray bs = new BitArray(AbstractBGPObjectParser.FLAGS_SIZE);
        bs.set(GLOBAL_LABEL, labelCase.getFlags().isGlobalLabel());
        bs.toByteBuf(bufferAux);
        bufferAux.writeByte(labelCase.getLabelCType());
        bufferAux.writeLong(labelCase.getLabel());
    }

    protected static ProtectionCase parseProtectionSubObject(final ByteBuf byteBuf) {
        ProtectionCaseBuilder builder = new ProtectionCaseBuilder();
        //skip reserved
        byteBuf.readByte();
        final short cType = byteBuf.readUnsignedByte();
        switch (cType) {
        case PROTECTION_SUBOBJECT_TYPE_1:
            builder.setCommonProtectionObject(BGPCommonExplicitRouteObjectParser.parseCommonProtectionBodyType1(byteBuf));
            break;
        case PROTECTION_SUBOBJECT_TYPE_2:
            builder.setCommonProtectionObject(BGPCommonExplicitRouteObjectParser.parseCommonProtectionBodyType2(byteBuf));
            break;
        default:
            LOG.warn("Secondary Record Route Protection Subobject cType {} not supported", cType);
            break;
        }
        return builder.build();
    }
}
