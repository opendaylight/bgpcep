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

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.error.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;

/**
 * Parser for {@link ErrorObject}
 */
public class PCEPErrorObjectParser extends AbstractObjectWithTlvsParser<ErrorObjectBuilder> {

    public static final int CLASS = 13;

    public static final int TYPE = 1;

    private static final int FLAGS_F_LENGTH = 1;

    private static final int RESERVED  = 1;

    public PCEPErrorObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public ErrorObject parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final ErrorObjectBuilder builder = new ErrorObjectBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        bytes.readerIndex(bytes.readerIndex() + FLAGS_F_LENGTH + RESERVED);
        builder.setType((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setValue((short) UnsignedBytes.toInt(bytes.readByte()));
        parseTlvs(builder, bytes.slice());
        return builder.build();
    }

    @Override
    public void addTlv(final ErrorObjectBuilder builder, final Tlv tlv) {
        if (tlv instanceof ReqMissing && builder.getType() == 7) {
            builder.setTlvs(new TlvsBuilder().setReqMissing((ReqMissing) tlv).build());
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof ErrorObject, "Wrong instance of PCEPObject. Passed %s. Needed ErrorObject.", object.getClass());
        final ErrorObject errObj = (ErrorObject) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(FLAGS_F_LENGTH + RESERVED);
        body.writeByte(errObj.getType());
        body.writeByte(errObj.getValue());
        // FIXME: switch to ByteBuf
        final byte[] tlvs = serializeTlvs(errObj.getTlvs());
        if (tlvs.length != 0) {
            body.writeBytes(tlvs);
        }
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public byte[] serializeTlvs(final Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        } else if (tlvs.getReqMissing() != null) {
            return serializeTlv(tlvs.getReqMissing());
        }
        return new byte[0];
    }
}
