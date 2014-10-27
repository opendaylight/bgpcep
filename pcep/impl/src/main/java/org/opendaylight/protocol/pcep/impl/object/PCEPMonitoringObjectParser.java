/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.Monitoring.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.MonitoringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.monitoring.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.monitoring.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;

/**
 * Parser for {@link Monitoring}
 * @see https://tools.ietf.org/html/rfc5886#section-4.1
 */
public class PCEPMonitoringObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

    public static final int CLASS = 19;

    public static final int TYPE = 1;

    private static final int UNASSIGNED_FLAGS = 2;
    private static final int RESERVED = 1;
    private static final int L_FLAG_POS = 7;
    private static final int G_FLAG_POS = 6;
    private static final int P_FLAG_POS = 5;
    private static final int C_FLAG_POS = 4;
    private static final int I_FLAG_POS = 3;

    public PCEPMonitoringObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final MonitoringBuilder builder = new MonitoringBuilder();
        buffer.readBytes(RESERVED + UNASSIGNED_FLAGS);
        final BitSet flagBits = ByteArray.bytesToBitSet(new byte[] { buffer.readByte() });
        final Flags flags = new Flags(flagBits.get(G_FLAG_POS), flagBits.get(I_FLAG_POS), flagBits.get(L_FLAG_POS),
                flagBits.get(C_FLAG_POS), flagBits.get(P_FLAG_POS));
        builder.setFlags(flags);
        builder.setMonitoringId(buffer.readUnsignedInt());
        final TlvsBuilder tbuilder = new TlvsBuilder();
        parseTlvs(tbuilder, buffer.slice());
        builder.setTlvs(tbuilder.build());
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Monitoring, "Wrong instance of PCEPObject. Passed %s. Needed MonitoringObject.", object.getClass());
        final Monitoring monitoring = (Monitoring) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(RESERVED + UNASSIGNED_FLAGS);
        final Flags flags = monitoring.getFlags();
        final BitSet flagBits = new BitSet(1);
        flagBits.set(I_FLAG_POS, flags.isIncomplete());
        flagBits.set(C_FLAG_POS, flags.isOverload());
        flagBits.set(P_FLAG_POS, flags.isProcessingTime());
        flagBits.set(G_FLAG_POS, flags.isGeneral());
        flagBits.set(L_FLAG_POS, flags.isLiveness());
        ByteBufWriteUtil.writeBitSet(flagBits, 1, body);
        ByteBufWriteUtil.writeUnsignedInt(monitoring.getMonitoringId(), body);
        serializeTlvs(monitoring.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    @Override
    protected void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
    }

}
