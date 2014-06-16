/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;

/**
 * Parser for {@link Lspa}
 */
public class PCEPLspaObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {

    public static final int CLASS = 9;

    public static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int FLAGS_F_LENGTH = 1;

    /*
     * offsets of flags inside flags field in bits
     */
    private static final int L_FLAG_OFFSET = 7;

    private static final int RESERVED = 1;

    public PCEPLspaObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Lspa parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final LspaBuilder builder = new LspaBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());

        builder.setExcludeAny(new AttributeFilter(bytes.readUnsignedInt()));
        builder.setIncludeAll(new AttributeFilter(bytes.readUnsignedInt()));
        builder.setIncludeAny(new AttributeFilter(bytes.readUnsignedInt()));
        builder.setSetupPriority((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setHoldPriority((short) UnsignedBytes.toInt(bytes.readByte()));

        final BitSet flags = ByteArray.bytesToBitSet(new byte[] { bytes.readByte() });
        builder.setLocalProtectionDesired(flags.get(L_FLAG_OFFSET));
        final TlvsBuilder tbuilder = new TlvsBuilder();
        bytes.readerIndex(bytes.readerIndex() + RESERVED);
        parseTlvs(tbuilder, bytes.slice());
        builder.setTlvs(tbuilder.build());
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Lspa, "Wrong instance of PCEPObject. Passed %s. Needed LspaObject.", object.getClass());
        final Lspa lspaObj = (Lspa) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeInt(lspaObj.getExcludeAny().getValue().intValue());
        body.writeInt(lspaObj.getIncludeAny().getValue().intValue());
        body.writeInt(lspaObj.getIncludeAll().getValue().intValue());
        body.writeByte(lspaObj.getSetupPriority());
        body.writeByte(lspaObj.getHoldPriority());
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        if (lspaObj.isLocalProtectionDesired() != null) {
            flags.set(L_FLAG_OFFSET, lspaObj.isLocalProtectionDesired());
        }
        body.writeBytes(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH));
        body.writeZero(RESERVED);
        serializeTlvs(lspaObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        return;
    }
}
