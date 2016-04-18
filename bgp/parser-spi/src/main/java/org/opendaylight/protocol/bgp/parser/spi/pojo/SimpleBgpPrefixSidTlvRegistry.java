/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SimpleBgpPrefixSidTlvRegistry implements BgpPrefixSidTlvRegistry {

    private final HandlerRegistry<DataContainer, BgpPrefixSidTlvParser, BgpPrefixSidTlvSerializer> handlers = new HandlerRegistry<>();

    AutoCloseable registerBgpPrefixSidTlvParser(final int tlvType, final BgpPrefixSidTlvParser parser) {
        Preconditions.checkArgument(tlvType >= 0 && tlvType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(tlvType, parser);
    }

    AutoCloseable registerBgpPrefixSidTlvSerializer(final Class<? extends BgpPrefixSidTlv> tlvClass, final BgpPrefixSidTlvSerializer serializer) {
        return this.handlers.registerSerializer(tlvClass, serializer);
    }

    @Override
    public BgpPrefixSidTlv parseBgpPrefixSidTlv(final int type, final ByteBuf buffer) {
        final BgpPrefixSidTlvParser parser = this.handlers.getParser(type);
        if (parser == null) {
            return null;
        }
        return parser.parseBgpPrefixSidTlv(buffer);
    }

    @Override
    public void serializeBgpPrefixSidTlv(final BgpPrefixSidTlv tlv, final ByteBuf bytes) {
        final BgpPrefixSidTlvSerializer serializer = this.handlers.getSerializer(tlv.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeBgpPrefixSidTlv(tlv, bytes);
    }

}
