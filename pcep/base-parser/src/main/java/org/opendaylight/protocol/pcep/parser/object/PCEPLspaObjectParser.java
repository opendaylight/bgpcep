/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.lspa.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.lspa.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link Lspa}.
 */
public class PCEPLspaObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {
    private static final int CLASS = 9;
    private static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int FLAGS_SIZE = 8;

    /*
     * offsets of flags inside flags field in bits
     */
    private static final int L_FLAG_OFFSET = 7;

    private static final int RESERVED = 1;

    public PCEPLspaObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public Lspa parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        final LspaBuilder builder = new LspaBuilder()
                .setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setExcludeAny(new AttributeFilter(ByteBufUtils.readUint32(bytes)))
                .setIncludeAll(new AttributeFilter(ByteBufUtils.readUint32(bytes)))
                .setIncludeAny(new AttributeFilter(ByteBufUtils.readUint32(bytes)))
                .setSetupPriority(ByteBufUtils.readUint8(bytes))
                .setHoldPriority(ByteBufUtils.readUint8(bytes));

        final BitArray flags = BitArray.valueOf(bytes.readByte());
        builder.setLocalProtectionDesired(flags.get(L_FLAG_OFFSET));
        final TlvsBuilder tbuilder = new TlvsBuilder();
        bytes.skipBytes(RESERVED);
        parseTlvs(tbuilder, bytes.slice());
        builder.setTlvs(tbuilder.build());
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Lspa,
            "Wrong instance of PCEPObject. Passed %s. Needed LspaObject.", object.getClass());
        final Lspa lspaObj = (Lspa) object;
        final ByteBuf body = Unpooled.buffer();
        writeAttributeFilter(lspaObj.getExcludeAny(), body);
        writeAttributeFilter(lspaObj.getIncludeAny(), body);
        writeAttributeFilter(lspaObj.getIncludeAll(), body);
        writeUnsignedByte(lspaObj.getSetupPriority(), body);
        writeUnsignedByte(lspaObj.getHoldPriority(), body);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(L_FLAG_OFFSET, lspaObj.isLocalProtectionDesired());
        flags.toByteBuf(body);
        body.writeZero(RESERVED);
        serializeTlvs(lspaObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
    }

    private static void writeAttributeFilter(final AttributeFilter attributeFilter, final ByteBuf body) {
        writeUnsignedInt(attributeFilter != null ? attributeFilter.getValue() : null, body);
    }

    @Override
    protected final void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
