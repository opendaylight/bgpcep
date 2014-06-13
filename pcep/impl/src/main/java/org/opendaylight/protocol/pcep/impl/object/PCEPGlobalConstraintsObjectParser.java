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

    private static final int MAX_HOP_F_LENGTH = 1;
    private static final int MAX_UTIL_F_LENGTH = 1;
    private static final int MIN_UTIL_F_LENGTH = 1;
    private static final int OVER_BOOKING_FACTOR_F_LENGTH = 1;

    private static final int MAX_HOP_F_OFFSET = 0;
    private static final int MAX_UTIL_F_OFFSET = MAX_HOP_F_OFFSET + MAX_HOP_F_LENGTH;
    private static final int MIN_UTIL_F_OFFSET = MAX_UTIL_F_OFFSET + MAX_UTIL_F_LENGTH;
    private static final int OVER_BOOKING_FACTOR_F_OFFSET = MIN_UTIL_F_OFFSET + MIN_UTIL_F_LENGTH;

    private static final int TLVS_OFFSET = OVER_BOOKING_FACTOR_F_OFFSET + OVER_BOOKING_FACTOR_F_LENGTH;

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
        if (!(object instanceof Gc)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed GcObject.");
        }
        final Gc specObj = (Gc) object;
        final byte[] retBytes = new byte[TLVS_OFFSET + 0];
        retBytes[MAX_HOP_F_OFFSET] = UnsignedBytes.checkedCast(specObj.getMaxHop());
        retBytes[MAX_UTIL_F_OFFSET] = UnsignedBytes.checkedCast(specObj.getMaxUtilization());
        retBytes[MIN_UTIL_F_OFFSET] = UnsignedBytes.checkedCast(specObj.getMinUtilization());
        retBytes[OVER_BOOKING_FACTOR_F_OFFSET] = UnsignedBytes.checkedCast(specObj.getOverBookingFactor());
        // FIXME: switch to ByteBuf
        buffer.writeBytes(ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes));
    }
}
