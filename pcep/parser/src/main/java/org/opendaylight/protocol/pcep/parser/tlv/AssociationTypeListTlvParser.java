/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.list.tlv.AssociationTypeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.list.tlv.AssociationTypeListBuilder;

/**
 * Parser for {@link AssociationTypeList}.
 */
public class AssociationTypeListTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 35;

    private static final int OF_CODE_ELEMENT_LENGTH = 2;

    @Override
    public AssociationTypeList parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() % OF_CODE_ELEMENT_LENGTH != 0) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + ".");
        }
        final var assocTypeList = ImmutableSet.<AssociationType>builder();
        while (buffer.isReadable()) {
            // Check that Association Type is valid
            final var associationType = AssociationType.forValue(buffer.readShort());
            if (associationType != null) {
                assocTypeList.add(associationType);
            }
        }
        return new AssociationTypeListBuilder().setAssociationType(assocTypeList.build()).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof AssociationTypeList, "AssociationTypeList TLV is mandatory.");
        final AssociationTypeList atl = (AssociationTypeList) tlv;
        final ByteBuf body = Unpooled.buffer();
        atl.getAssociationType().forEach(id -> body.writeShort(id.getIntValue()));
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

}
