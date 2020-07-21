/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class SrPceCapabilityTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 26;

    private static final int BITSET_LENGTH = 8;
    private static final int N_FLAG_POSITION = 7;
    private static final int X_FLAG_POSITION = 6;
    private static final int CONTENT_LENGTH = 4;
    private static final int OFFSET = 2;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof SrPceCapability, "SrPceCapability is mandatory.");
        SrPceCapability srPceCapability = (SrPceCapability )tlv;
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        final BitArray bits = new BitArray(BITSET_LENGTH);

        /* Reserved 2 bytes */
        body.writerIndex(OFFSET);

        /* Flags */
        bits.set(N_FLAG_POSITION, srPceCapability.isNFlag());
        bits.set(X_FLAG_POSITION, srPceCapability.isXFlag());
        bits.toByteBuf(body);

        /* MSD */
        ByteBufUtils.writeOrZero(body, srPceCapability.getMsd());

        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final BitArray bitSet = BitArray.valueOf(buffer.readerIndex(OFFSET).readByte());
        final boolean n = bitSet.get(N_FLAG_POSITION);
        final boolean x = bitSet.get(X_FLAG_POSITION);

        return new SrPceCapabilityBuilder()
                .setNFlag(n)
                .setXFlag(x)
                .setMsd(ByteBufUtils.readUint8(buffer))
                .build();
    }
}
