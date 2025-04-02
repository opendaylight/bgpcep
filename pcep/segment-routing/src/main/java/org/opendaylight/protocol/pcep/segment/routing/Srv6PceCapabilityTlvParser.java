/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.Srv6PceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.srv6.pce.capability.Msds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.srv6.pce.capability.MsdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.Tlv;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class Srv6PceCapabilityTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 27;

    private static final int BITSET_LENGTH = 16;
    private static final int N_FLAG_POSITION = 14;
    private static final int RESERVED = 2;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof Srv6PceCapability, "Srv6PceCapability is mandatory.");
        final ByteBuf body = Unpooled.buffer();

        /* Reserved 2 bytes */
        body.writeZero(RESERVED);

        /* Flags */
        final Srv6PceCapability srv6PceCapability = (Srv6PceCapability) tlv;
        final BitArray bits = new BitArray(BITSET_LENGTH);
        bits.set(N_FLAG_POSITION, srv6PceCapability.getNFlag());
        bits.toByteBuf(body);

        /* MSDs */
        if (srv6PceCapability.getMsds() != null) {
            srv6PceCapability.getMsds().forEach(msd -> {
                ByteBufUtils.writeUint8(body, msd.getMsdType());
                ByteBufUtils.writeUint8(body, msd.getMsdValue());
            });
        }

        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }

        // Skip reserved
        buffer.skipBytes(RESERVED);
        final Srv6PceCapabilityBuilder srv6PceCapa = new Srv6PceCapabilityBuilder()
            .setNFlag(BitArray.valueOf(buffer, BITSET_LENGTH).get(N_FLAG_POSITION));

        /* MSDs */
        if (buffer.isReadable()) {
            final List<Msds> msds = new ArrayList<Msds>();
            while (buffer.isReadable()) {
                final var type = ByteBufUtils.readUint8(buffer);
                // Check if Type / Value is valid i.e. not padding
                if (type != Uint8.ZERO) {
                    msds.add(new MsdsBuilder().setMsdType(type).setMsdValue(ByteBufUtils.readUint8(buffer)).build());
                }
            }
            srv6PceCapa.setMsds(msds);
        }

        return srv6PceCapa.build();
    }
}
