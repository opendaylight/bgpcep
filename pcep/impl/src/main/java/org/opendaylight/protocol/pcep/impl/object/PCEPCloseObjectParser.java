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

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.c.close.Tlvs;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPCloseObject PCEPCloseObject}
 */
public class PCEPCloseObjectParser extends AbstractObjectWithTlvsParser<CCloseBuilder> {

    public static final int CLASS = 15;

    public static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int FLAGS_F_LENGTH = 1;
    private static final int REASON_F_LENGTH = 1;

    /*
     * offsets of fields in bytes
     */
    private static final int FLAGS_F_OFFSET = 2;
    private static final int REASON_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;

    /*
     * total size of object in bytes
     */
    private static final int TLVS_OFFSET = REASON_F_OFFSET + REASON_F_LENGTH;

    public PCEPCloseObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public CClose parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final CCloseBuilder builder = new CCloseBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        bytes.readerIndex(bytes.readerIndex() + REASON_F_OFFSET);
        builder.setReason((short) UnsignedBytes.toInt(bytes.readByte()));
        parseTlvs(builder, bytes.slice());
        return builder.build();
    }

    @Override
    public byte[] serializeObject(final Object object) {
        if (!(object instanceof CClose)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed CloseObject.");
        }
        final CClose obj = (CClose) object;

        final byte[] tlvs = serializeTlvs(obj.getTlvs());
        int tlvsLength = 0;
        if (tlvs != null) {
            tlvsLength = tlvs.length;
        }
        final byte[] retBytes = new byte[TLVS_OFFSET + tlvsLength + getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

        if (tlvs != null) {
            ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
        }
        retBytes[REASON_F_OFFSET] = UnsignedBytes.checkedCast(obj.getReason());
        return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
    }

    public byte[] serializeTlvs(final Tlvs tlvs) {
        // No tlvs defined
        return new byte[0];
    }
}
