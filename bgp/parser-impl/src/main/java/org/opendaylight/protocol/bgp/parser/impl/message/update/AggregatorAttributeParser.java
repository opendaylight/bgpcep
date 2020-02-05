/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ShortAsNumber;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser/serializer for {@link Aggregator}.
 */
public final class AggregatorAttributeParser extends AbstractAttributeParser implements AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AggregatorAttributeParser.class);
    private static final int AGGREGATOR_LENGTH = 8;
    public static final int TYPE = 7;

    private final ReferenceCache refCache;

    public AggregatorAttributeParser(final ReferenceCache refCache) {
        this.refCache = requireNonNull(refCache);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint) {
        if (buffer.readableBytes() != AGGREGATOR_LENGTH && errorHandling != RevisedErrorHandling.NONE) {
            // RFC7606: we do not support non-4-octet AS number peers, perform attribute-discard
            LOG.debug("Discarded malformed AGGREGATOR attribute");
            return;
        }

        builder.setAggregator(new AggregatorBuilder()
            // FIXME: above check should be expanded, so we report at least underflow errors
            .setAsNumber(this.refCache.getSharedReference(new AsNumber(ByteBufUtils.readUint32(buffer))))
            .setNetworkAddress(Ipv4Util.addressForByteBuf(buffer))
            .build());
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final Aggregator aggregator = pathAttributes.getAggregator();
        if (aggregator != null) {
            final AsNumber asNumber = aggregator.getAsNumber();
            if (asNumber != null) {
                AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL | AttributeUtil.TRANSITIVE, TYPE,
                    Unpooled.buffer(AGGREGATOR_LENGTH)
                        .writeInt(new ShortAsNumber(asNumber).getValue().intValue())
                        .writeBytes(Ipv4Util.bytesForAddress(aggregator.getNetworkAddress())),
                        byteAggregator);
            }
        }
    }
}
