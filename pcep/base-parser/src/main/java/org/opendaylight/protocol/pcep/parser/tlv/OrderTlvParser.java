/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.order.tlv.OrderBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link Order}.
 */
public class OrderTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 5;

    @Override
    public Order parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        return new OrderBuilder()
                .setDelete(ByteBufUtils.readUint32(buffer))
                .setSetup(ByteBufUtils.readUint32(buffer))
                .build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof Order, "OrderTlv is mandatory.");
        final Order otlv = (Order) tlv;
        final ByteBuf body = Unpooled.buffer();
        writeUnsignedInt(otlv.getDelete(), body);
        writeUnsignedInt(otlv.getSetup(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
