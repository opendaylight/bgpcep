/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AtomicAggregateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AtomicAggregateAttributeParser extends AbstractAttributeParser implements AttributeSerializer {
    public static final int TYPE = 6;
    private static final Logger LOG = LoggerFactory.getLogger(AtomicAggregateAttributeParser.class);
    private static final AtomicAggregate ATOMIC_AGGREGATE = new AtomicAggregateBuilder().build();

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint) {
        if (!buffer.isReadable() && errorHandling != RevisedErrorHandling.NONE) {
            LOG.debug("Discarded malformed ATOMIC_AGGREGATE attribute");
            return;
        }

        // XXX: reject malformed attributes?
        builder.setAtomicAggregate(ATOMIC_AGGREGATE);
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes.getAtomicAggregate() != null) {
            AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, Unpooled.EMPTY_BUFFER, byteAggregator);
        }
    }
}
