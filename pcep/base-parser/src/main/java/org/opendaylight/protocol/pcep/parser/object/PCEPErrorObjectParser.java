/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.error.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link ErrorObject}.
 */
public final class PCEPErrorObjectParser extends AbstractObjectWithTlvsParser<ErrorObjectBuilder> {
    private static final int CLASS = 13;
    private static final int TYPE = 1;
    private static final int FLAGS_F_LENGTH = 1;
    private static final int RESERVED  = 1;

    public PCEPErrorObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public ErrorObject parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        bytes.skipBytes(FLAGS_F_LENGTH + RESERVED);
        final ErrorObjectBuilder builder = new ErrorObjectBuilder()
                .setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setType(ByteBufUtils.readUint8(bytes))
                .setValue(ByteBufUtils.readUint8(bytes));
        parseTlvs(builder, bytes.slice());
        return builder.build();
    }

    @Override
    public void addTlv(final ErrorObjectBuilder builder, final Tlv tlv) {
        if (tlv instanceof ReqMissing
                && PCEPErrors.SYNC_PATH_COMP_REQ_MISSING.getErrorType().equals(builder.getType())) {
            builder.setTlvs(new TlvsBuilder().setReqMissing((ReqMissing) tlv).build());
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof ErrorObject,
            "Wrong instance of PCEPObject. Passed %s. Needed ErrorObject.", object.getClass());
        final ErrorObject errObj = (ErrorObject) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(FLAGS_F_LENGTH + RESERVED);
        Preconditions.checkArgument(errObj.getType() != null, "Type is mandatory.");
        writeUnsignedByte(errObj.getType(), body);
        Preconditions.checkArgument(errObj.getValue() != null, "Value is mandatory.");
        writeUnsignedByte(errObj.getValue(), body);
        serializeTlvs(errObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        if (tlvs.getReqMissing() != null) {
            serializeTlv(tlvs.getReqMissing(), body);
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
    }

    @Override
    protected void addVendorInformationTlvs(
        final ErrorObjectBuilder builder,
        final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setTlvs(new TlvsBuilder(builder.getTlvs()).setVendorInformationTlv(tlvs).build());
        }
    }
}
