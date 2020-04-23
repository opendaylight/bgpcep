/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.UnrecognizedAttributesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnrecognizedAttributesSerializer implements AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(UnrecognizedAttributesSerializer.class);

    @Override
    public void serializeAttribute(final Attributes attributes, final ByteBuf byteAggregator) {
        final Map<UnrecognizedAttributesKey, UnrecognizedAttributes> unrecognizedAttrs =
                attributes.getUnrecognizedAttributes();
        if (unrecognizedAttrs == null) {
            return;
        }
        for (final UnrecognizedAttributes unrecognizedAttr : unrecognizedAttrs.values()) {
            LOG.trace("Serializing unrecognized attribute of type {}", unrecognizedAttr.getType());
            int flags = AttributeUtil.OPTIONAL;
            if (unrecognizedAttr.isPartial()) {
                flags |= AttributeUtil.PARTIAL;
            }
            if (unrecognizedAttr.isTransitive()) {
                flags |= AttributeUtil.TRANSITIVE;
            }
            AttributeUtil.formatAttribute(flags, unrecognizedAttr.getType().toJava(),
                    Unpooled.wrappedBuffer(unrecognizedAttr.getValue()), byteAggregator);
        }
    }

}
