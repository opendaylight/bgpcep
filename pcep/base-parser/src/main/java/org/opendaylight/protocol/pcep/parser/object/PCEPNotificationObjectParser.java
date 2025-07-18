/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.notification.object.c.notification.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.notification.object.c.notification.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.tlvs.VendorInformationTlv;
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
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        bytes.skipBytes(NT_F_OFFSET);
        final CNotificationBuilder builder = new CNotificationBuilder()
                .setIgnore(header.getIgnore())
                .setProcessingRule(header.getProcessingRule())
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
        checkArgument(object instanceof CNotification,
            "Wrong instance of PCEPObject. Passed %s. Needed CNotificationObject.", object.getClass());
        final CNotification notObj = (CNotification) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(NT_F_OFFSET);
        ByteBufUtils.writeMandatory(body, notObj.getType(), "Type");
        ByteBufUtils.writeMandatory(body, notObj.getValue(), "Value");
        serializeTlvs(notObj.getTlvs(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }

    @NonNullByDefault
    public void serializeTlvs(final @Nullable Tlvs tlvs, final ByteBuf body) {
        if (tlvs != null) {
            serializeOptionalTlv(tlvs.getOverloadDuration(), body);
            serializeVendorInformationTlvs(tlvs.getVendorInformationTlv(), body);
        }
    }

    @Override
    protected void addVendorInformationTlvs(final CNotificationBuilder builder, final List<VendorInformationTlv> tlvs) {
        if (!tlvs.isEmpty()) {
            builder.setTlvs(new TlvsBuilder(builder.getTlvs()).setVendorInformationTlv(tlvs).build());
        }
    }
}
