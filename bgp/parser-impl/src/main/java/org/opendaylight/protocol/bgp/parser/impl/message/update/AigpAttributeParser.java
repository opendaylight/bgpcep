/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Aigp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AigpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.aigp.AigpTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.aigp.AigpTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AigpAttributeParser implements AttributeParser, AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AigpAttributeParser.class);
    private static final byte AIGP_TLV_TYPE = 1;
    private static final short AIGP_TLV_SIZE = 11;
    private static final int TLV_SIZE_IN_BYTES = 2;

    public static final int TYPE = 26;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final PeerSpecificParserConstraint constraint) {
        if (buffer.isReadable()) {
            builder.setAigp(new AigpBuilder().setAigpTlv(parseAigpTLV(buffer)).build());
        }
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final Aigp aigpAttribute = attribute.getAigp();
        if (aigpAttribute != null) {
            AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, serializeAigpTLV(aigpAttribute),
                byteAggregator);
        }
    }

    /**
     * Reads data from buffer until reaches TLV of type AIGP TLV.
     *
     * @param buffer TLVs in ByteBuf format
     * @return instance of AigpTlv class or null if buffer contains unknown TLV type
     */
    private static AigpTlv parseAigpTLV(final ByteBuf buffer) {
        final int tlvType = buffer.readByte();
        buffer.skipBytes(TLV_SIZE_IN_BYTES);
        if (tlvType != AIGP_TLV_TYPE) {
            LOG.warn("AIGP attribute contains unknown TLV type {}.", tlvType);
            return null;
        }
        return new AigpTlvBuilder().setMetric(new AccumulatedIgpMetric(ByteBufUtils.readUint64(buffer))).build();
    }

    /**
     * Transform AIGP attribute data from instance of Aigp class into byte buffer representation.
     *
     * @param aigp instance of Aigp class
     * @return byte buffer representation or empty buffer if AIGP TLV is null
     */
    private static ByteBuf serializeAigpTLV(final Aigp aigp) {
        final AigpTlv tlv = aigp.getAigpTlv();
        if (tlv == null) {
            return Unpooled.EMPTY_BUFFER;
        }
        return Unpooled.buffer(AIGP_TLV_SIZE)
                .writeByte(AIGP_TLV_TYPE)
                .writeShort(AIGP_TLV_SIZE)
                .writeLong(tlv.getMetric().getValue().longValue());
    }
}
