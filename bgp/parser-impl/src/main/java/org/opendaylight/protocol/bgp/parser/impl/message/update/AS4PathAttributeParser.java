/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class AS4PathAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 18;

    @Override
    public void parseAttribute(final ByteBuf bytes, final AttributesBuilder builder) {
        // AS4 Path is ignored
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        // AS4 Path is ignored
    }
}
