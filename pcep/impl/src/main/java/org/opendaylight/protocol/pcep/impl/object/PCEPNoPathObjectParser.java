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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;

/**
 * Parser for {@link NoPath}
 */
public class PCEPNoPathObjectParser extends AbstractObjectWithTlvsParser<NoPathBuilder> {

    public static final int CLASS = 3;

    public static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int FLAGS_F_LENGTH = 2;
    private static final int RESERVED_F_LENGTH = 1;

    /*
     * defined flags
     */
    private static final int C_FLAG_OFFSET = 0;

    public PCEPNoPathObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public NoPath parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final NoPathBuilder builder = new NoPathBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());

        builder.setNatureOfIssue((short) UnsignedBytes.toInt(bytes.readByte()));
        final byte[] flagsByte = ByteArray.readBytes(bytes, FLAGS_F_LENGTH);
        final BitSet flags = ByteArray.bytesToBitSet(flagsByte);
        builder.setUnsatisfiedConstraints(flags.get(C_FLAG_OFFSET));
        bytes.readerIndex(bytes.readerIndex() + RESERVED_F_LENGTH);
        parseTlvs(builder, bytes.slice());
        return builder.build();
    }

    @Override
    public void addTlv(final NoPathBuilder builder, final Tlv tlv) {
        if (tlv instanceof NoPathVector) {
            builder.setTlvs(new TlvsBuilder().setNoPathVector((NoPathVector) tlv).build());
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof NoPath, String.format("Wrong instance of PCEPObject. Passed %s . Needed NoPathObject.", object.getClass()));
        final NoPath nPObj = (NoPath) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeByte(nPObj.getNatureOfIssue());
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        if (nPObj.isUnsatisfiedConstraints() != null) {
            flags.set(C_FLAG_OFFSET, nPObj.isUnsatisfiedConstraints());
        }
        body.writeBytes(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH));
        body.writeZero(RESERVED_F_LENGTH);
        // FIXME: switch to ByteBuf
        final byte[] tlvs = serializeTlvs(nPObj.getTlvs());
        if (tlvs != null) {
            body.writeBytes(tlvs);
        }
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public byte[] serializeTlvs(final Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        } else if (tlvs.getNoPathVector() != null) {
            return serializeTlv(tlvs.getNoPathVector());
        }
        return new byte[0];
    }
}
