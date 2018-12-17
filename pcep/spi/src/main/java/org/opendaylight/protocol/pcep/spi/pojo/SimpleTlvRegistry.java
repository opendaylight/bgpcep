/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SimpleTlvRegistry implements TlvRegistry {

    private final HandlerRegistry<DataContainer, TlvParser, TlvSerializer> handlers = new HandlerRegistry<>();

    public AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
        Preconditions.checkArgument(tlvType >= 0 && tlvType < Values.UNSIGNED_SHORT_MAX_VALUE);
        return this.handlers.registerParser(tlvType, parser);
    }

    public AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
        return this.handlers.registerSerializer(tlvClass, serializer);
    }

    @Override
    public Tlv parseTlv(final int type, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(type >= 0 && type <= Values.UNSIGNED_SHORT_MAX_VALUE);
        final TlvParser parser = this.handlers.getParser(type);
        if (parser == null) {
            return null;
        }
        return parser.parseTlv(buffer);
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        final TlvSerializer serializer = this.handlers.getSerializer(tlv.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeTlv(tlv, buffer);
    }
}
