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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.Gc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.GcBuilder;

/**
 * Parser for {@link Gc}
 */
public class PCEPGlobalConstraintsObjectParser extends AbstractObjectWithTlvsParser<GcBuilder> {

    public static final int CLASS = 24;

    public static final int TYPE = 1;

    public PCEPGlobalConstraintsObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Gc parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final GcBuilder builder = new GcBuilder();

        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());

        builder.setMaxHop((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setMaxUtilization((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setMinUtilization((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setOverBookingFactor((short) UnsignedBytes.toInt(bytes.readByte()));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Gc, String.format("Wrong instance of PCEPObject. Passed %s . Needed GcObject.", object.getClass()));
        final Gc specObj = (Gc) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeByte(specObj.getMaxHop());
        body.writeByte(specObj.getMaxUtilization());
        body.writeByte(specObj.getMinUtilization());
        body.writeByte(specObj.getOverBookingFactor());
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
