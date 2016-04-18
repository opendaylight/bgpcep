/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class BgpPrefixSidAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 40;

    //    private static final int LABEL_INDEX_TYPE = 1;
    //    private static final int IPV6_SID_TYPE = 2;
    //    private static final int ORIGINATOR_SRGB_TYPE = 3;

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {

    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPDocumentedException, BGPParsingException {
        // <type, len, val>
        //        final byte type = buffer.readByte();
        //        final short length = buffer.readUnsignedByte();
        // type extract from builder
    }

}
