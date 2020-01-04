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
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.c.notification.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.c.notification.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link CNotification}.
 */
public final class PCEPNotificationObjectParser extends AbstractObjectWithTlvsParser<CNotificationBuilder> {
    private static final int CLASS = 12;
    private static final int TYPE = 1;

    /*
     * offsets of fields
     */
    private static final int NT_F_OFFSET = 2;

    public PCEPNotificationObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg, CLASS, TYPE);
    }

    @Override
    public CNotification parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        bytes.skipBytes(NT_F_OFFSET);
        final CNotificationBuilder builder = new CNotificationBuilder()
                .setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setType(ByteBufUtils.readUint8(bytes))
                .setValue(ByteBufUtils.readUint8(bytes));
        parseTlvs(builder, bytes.slice());
        return builder.build();
    }

    @Override
    public void addTlv(final CNotificationBuilder builder, final Tlv tlv) {
        if (tlv instanceof OverloadDuration && builder.getType().toJava() == 2 && builder.getValue().toJava() == 1) {
            builder.setTlvs(new TlvsBuilder().setOverloadDuration((OverloadDuration) tlv).build());
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof CNotification,
            "Wrong instance of PCEPObject. Passed %s. Needed CNotificationObject.",
            object.getClass());
        final CNotification notObj = (CNotification) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(NT_F_OFFSET);
        Preconditions.checkArgument(notObj.getType() != null, "Type is mandatory.");
        writeUnsignedByte(notObj.getType(), body);
        Preconditions.checkArgument(notObj.getValue() != null, "Value is mandatory.");
        writeUnsignedByte(notObj.getValue(), body);
        serializeTlvs(notObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        if (tlvs.getOverloadDuration() != null) {
            serializeTlv(tlvs.getOverloadDuration(), body);
        }
        serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
    }

    @Override
    protected void addVendorInformationTlvs(
        final CNotificationBuilder builder,
        final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setTlvs(new TlvsBuilder(builder.getTlvs()).setVendorInformationTlv(tlvs).build());
        }
    }
}
