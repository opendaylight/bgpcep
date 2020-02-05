/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;

public final class OriginAttributeParser extends AbstractAttributeParser implements AttributeSerializer {

    public static final int TYPE = 1;

    private static final Origin IGP = new OriginBuilder().setValue(BgpOrigin.Igp).build();
    private static final Origin EGP = new OriginBuilder().setValue(BgpOrigin.Egp).build();
    private static final Origin INC = new OriginBuilder().setValue(BgpOrigin.Incomplete).build();

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPDocumentedException, BGPTreatAsWithdrawException {
        final int readable = buffer.readableBytes();
        if (readable != 1) {
            throw errorHandling.reportError(BGPError.ATTR_LENGTH_ERROR,
                "ORIGIN attribute is expected to have size 1, but has %s", readable);
        }

        final byte rawOrigin = buffer.readByte();
        final BgpOrigin borigin = BgpOrigin.forValue(UnsignedBytes.toInt(rawOrigin));
        if (borigin == null) {
            throw errorHandling.reportError(BGPError.ORIGIN_ATTR_NOT_VALID, "Unknown ORIGIN type %s", rawOrigin);
        }
        switch (borigin) {
            case Egp:
                builder.setOrigin(EGP);
                return;
            case Igp:
                builder.setOrigin(IGP);
                return;
            case Incomplete:
                builder.setOrigin(INC);
                return;
            default:
        }
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final Origin origin = attribute.getOrigin();
        if (origin != null) {
            AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE,
                Unpooled.wrappedBuffer(new byte[]{ UnsignedBytes.checkedCast(origin.getValue().getIntValue()) }),
                byteAggregator);
        }
    }
}
