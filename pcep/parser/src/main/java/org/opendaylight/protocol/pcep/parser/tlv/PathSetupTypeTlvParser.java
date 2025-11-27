/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.tlv.PathSetupTypeBuilder;

public class PathSetupTypeTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 28;

    private static final int RESERVED = 3;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof PathSetupType, "PathSetupType is mandatory.");
        final PathSetupType pstTlv = (PathSetupType) tlv;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(RESERVED);
        body.writeByte(pstTlv.getPst().getIntValue());
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        buffer.skipBytes(RESERVED);
        final int pst = buffer.readByte();
        final var psType = PsType.forValue(pst);
        if (psType == null) {
            throw new PCEPDeserializerException("Unsuported Path Setup Type: " + pst);
        }
        return new PathSetupTypeBuilder().setPst(psType).build();
    }
}
