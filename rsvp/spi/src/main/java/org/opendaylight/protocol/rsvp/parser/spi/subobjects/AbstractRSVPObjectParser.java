/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractRSVPObjectParser implements RSVPTeObjectSerializer, RSVPTeObjectParser {

    protected static final int METRIC_VALUE_F_LENGTH = 4;
    protected static final int OVERALL_LENGTH = 7;
    protected static final int PARAMETER_127_LENGTH = 5;
    protected static final int PARAMETER_130_LENGTH = 2;
    protected static final int GUARANTEED_SERVICE_RSPEC = 130;
    protected static final int TOKEN_BUCKET_TSPEC = 127;
    protected static final int FLAGS_SIZE = 8;

    protected static void serializeAttributeHeader(final Integer valueLength, final short classNum,
        final short ctype, final ByteBuf byteAggregator) {
        byteAggregator.writeShort(valueLength);
        byteAggregator.writeByte(classNum);
        byteAggregator.writeByte(ctype);
    }

    protected static void writeAttributeFilter(final AttributeFilter attributeFilter, final ByteBuf body) {
        if (attributeFilter != null) {
            ByteBufUtils.write(body, attributeFilter.getValue());
        } else {
            body.writeInt(0);
        }
    }

    @Override
    public RsvpTeObject parseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        if (byteBuf == null) {
            return null;
        }
        return localParseObject(byteBuf);
    }

    @Override
    public void serializeObject(final RsvpTeObject rsvpTeObject, final ByteBuf output) {
        if (rsvpTeObject == null) {
            return;
        }
        localSerializeObject(rsvpTeObject, output);
    }

    protected abstract void localSerializeObject(RsvpTeObject rsvpTeObject, ByteBuf output);

    protected abstract RsvpTeObject localParseObject(ByteBuf byteBuf) throws RSVPParsingException;
}
