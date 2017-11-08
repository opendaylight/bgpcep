/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.registry;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBmpTlvRegistry implements BmpTlvRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleBmpTlvRegistry.class);

    private final HandlerRegistry<DataContainer, BmpTlvParser, BmpTlvSerializer> handlers = new HandlerRegistry<>();

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf output) {
        final BmpTlvSerializer serializer = this.handlers.getSerializer(tlv.getImplementedInterface());
        if (serializer == null) {
            LOG.warn("BMP serializer for TLV type {} is not registered.", tlv.getClass());
            return;
        }
        serializer.serializeTlv(tlv, output);
    }

    @Override
    public Tlv parseTlv(final int tlvType, final ByteBuf buffer) throws BmpDeserializationException {
        Preconditions.checkArgument(tlvType >= 0 && tlvType <= Values.UNSIGNED_SHORT_MAX_VALUE);
        final BmpTlvParser parser = this.handlers.getParser(tlvType);
        if (parser == null) {
            LOG.warn("BMP parser for TLV type {} is not registered.", tlvType);
            return null;
        }
        return parser.parseTlv(buffer);
    }

    @Override
    public AutoCloseable registerBmpTlvParser(final int tlvType, final BmpTlvParser parser) {
        Preconditions.checkArgument(tlvType >= 0 && tlvType < Values.UNSIGNED_SHORT_MAX_VALUE);
        return this.handlers.registerParser(tlvType, parser);
    }

    @Override
    public AutoCloseable registerBmpTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return this.handlers.registerSerializer(tlvClass, serializer);
    }

}
