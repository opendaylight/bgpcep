/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.range.tlv.AssociationRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.range.tlv.AssociationRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.range.tlv.association.range.AssociationRanges;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.range.tlv.association.range.AssociationRangesBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link AssociationRange}.
 */
public class AssociationRangeTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 29;

    private static final int OF_CODE_ELEMENT_LENGTH = 8;
    private static final int RESERVED = 2;

    @Override
    public AssociationRange parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() % OF_CODE_ELEMENT_LENGTH != 0) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + ".");
        }
        final var assocRangeList = new ArrayList<AssociationRanges>();
        while (buffer.isReadable()) {
            buffer.skipBytes(RESERVED);
            final var type = AssociationType.forValue(buffer.readShort());
            final var start = ByteBufUtils.readUint16(buffer);
            final var range = ByteBufUtils.readUint16(buffer);
            if (type != null
                && (start.intValue() > 0 && start.intValue() < 0xffff)
                && range.intValue() > 0
                && (start.intValue() + range.intValue() < 0xfff)) {
                assocRangeList.add(new AssociationRangesBuilder()
                    .setAssociationType(type)
                    .setAssociationIdStart(start)
                    .setRange(range)
                    .build());
            } else {
                // FIXME: Should Trigger a PCEP Error as per https://tools.ietf.org/html/rfc8697#section-5.1
                throw new PCEPDeserializerException("Wrong Association Range type: " + type + " start: "
                   + start + "range: " + range + ".");
            }
        }
        return new AssociationRangeBuilder().setAssociationRanges(assocRangeList).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof AssociationRange, "AssociationRange TLV is mandatory.");
        final AssociationRange arl = (AssociationRange) tlv;
        final ByteBuf body = Unpooled.buffer();
        arl.getAssociationRanges().forEach(id -> {
            body.writeZero(RESERVED);
            body.writeShort(id.getAssociationType().getIntValue());
            ByteBufUtils.write(body, id.getAssociationIdStart());
            ByteBufUtils.write(body, id.getRange());
        });
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

}
