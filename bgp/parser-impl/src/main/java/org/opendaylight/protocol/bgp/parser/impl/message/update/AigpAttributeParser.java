/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Aigp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class AigpAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 26;

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder)
            throws BGPDocumentedException, BGPParsingException {
        builder.setAigp(AigpTransformerUtil.byteArrayToAigp(buffer));
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes,
                "Attribute parameter is not a PathAttribute object.");
        final Aigp aigpAttribute = ((PathAttributes) attribute).getAigp();
        if (aigpAttribute == null) {
            return;
        }
        final ByteBuf aigpAsByteBuf = AigpTransformerUtil.aigpToByteArray(aigpAttribute);
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, aigpAsByteBuf, byteAggregator);
    }
}
