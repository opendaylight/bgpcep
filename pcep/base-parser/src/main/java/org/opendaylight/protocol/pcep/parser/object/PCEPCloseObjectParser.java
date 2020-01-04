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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.c.close.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.c.close.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link
 *     org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.CClose
 *     PCEPCloseObject
 * }.
 */
public final class PCEPCloseObjectParser extends AbstractObjectWithTlvsParser<TlvsBuilder> {
    private static final int CLASS = 15;
    private static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int RESERVED = 2;
    private static final int FLAGS_F_LENGTH = 1;

    public PCEPCloseObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public CClose parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        bytes.skipBytes(FLAGS_F_LENGTH + RESERVED);
        final Uint8 reason = ByteBufUtils.readUint8(bytes);
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parseTlvs(tlvsBuilder, bytes.slice());
        final CCloseBuilder builder = new CCloseBuilder()
                .setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setReason(reason)
                .setTlvs(tlvsBuilder.build());
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof CClose, "Wrong instance of PCEPObject. Passed %s. Needed CCloseObject.",
            object.getClass());
        final CClose obj = (CClose) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(RESERVED + FLAGS_F_LENGTH);
        checkArgument(obj.getReason() != null, "Reason is mandatory.");
        writeUnsignedByte(obj.getReason(), body);
        serializeTlvs(obj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
    }

    @Override
    protected void addVendorInformationTlvs(final TlvsBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setVendorInformationTlv(tlvs);
        }
    }
}
