/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfListBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link OfList}.
 */
public class OFListTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 4;

    private static final int OF_CODE_ELEMENT_LENGTH = 2;

    @Override
    public OfList parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() % OF_CODE_ELEMENT_LENGTH != 0) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + ".");
        }
        final var ofCodes = ImmutableSet.<OfId>builder();
        while (buffer.isReadable()) {
            ofCodes.add(new OfId(ByteBufUtils.readUint16(buffer)));
        }
        return new OfListBuilder().setCodes(ofCodes.build()).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof OfList, "OFListTlv is mandatory.");
        final OfList oft = (OfList) tlv;
        final ByteBuf body = Unpooled.buffer();
        for (OfId id : oft.getCodes()) {
            ByteBufUtils.write(body, id.getValue());
        }
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
