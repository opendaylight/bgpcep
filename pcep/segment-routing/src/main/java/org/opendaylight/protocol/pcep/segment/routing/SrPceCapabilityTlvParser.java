/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class SrPceCapabilityTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 26;

    private static final int MSD_LENGTH = 1;
    private static final int CONTENT_LENGTH = 4;
    private static final int OFFSET = CONTENT_LENGTH - MSD_LENGTH;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof SrPceCapability, "SrPceCapability is mandatory.");
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        body.writerIndex(OFFSET);
        writeUnsignedByte(((SrPceCapability) tlv).getMsd(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        return new SrPceCapabilityBuilder()
                .setMsd(ByteBufUtils.readUint8(buffer.readerIndex(OFFSET)))
                .build();
    }
}
