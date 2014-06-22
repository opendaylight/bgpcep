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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.c.notification.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.c.notification.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;

/**
 * Parser for {@link CNotification}
 */
public class PCEPNotificationObjectParser extends AbstractObjectWithTlvsParser<CNotificationBuilder> {

    public static final int CLASS = 12;

    public static final int TYPE = 1;

    /*
     * offsets of fields
     */
    private static final int NT_F_OFFSET = 2;

    public PCEPNotificationObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public CNotification parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final CNotificationBuilder builder = new CNotificationBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        bytes.readerIndex(bytes.readerIndex() + NT_F_OFFSET);
        builder.setType((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setValue((short) UnsignedBytes.toInt(bytes.readByte()));
        parseTlvs(builder, bytes.slice());
        return builder.build();
    }

    @Override
    public void addTlv(final CNotificationBuilder builder, final Tlv tlv) {
        if (tlv instanceof OverloadDuration && builder.getType() == 2 && builder.getValue() == 1) {
            builder.setTlvs(new TlvsBuilder().setOverloadDuration((OverloadDuration) tlv).build());
        }
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof CNotification, "Wrong instance of PCEPObject. Passed %s. Needed CNotificationObject.", object.getClass());
        final CNotification notObj = (CNotification) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(NT_F_OFFSET);
        body.writeByte(notObj.getType());
        body.writeByte(notObj.getValue());
        // FIXME: switch to ByteBuf
        final byte[] tlvs = serializeTlvs(notObj.getTlvs());
        if (tlvs.length != 0) {
            body.writeBytes(tlvs);
        }
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public byte[] serializeTlvs(final Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        } else if (tlvs.getOverloadDuration() != null) {
            return serializeTlv(tlvs.getOverloadDuration());
        }
        return new byte[0];
    }
}
