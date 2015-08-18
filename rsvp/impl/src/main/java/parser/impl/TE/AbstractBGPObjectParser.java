/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.impl.TE;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;
import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import parser.spi.RSVPParsingException;
import parser.spi.RSVPTeObjectParser;
import parser.spi.RSVPTeObjectSerializer;

public abstract class AbstractBGPObjectParser implements RSVPTeObjectParser, RSVPTeObjectSerializer {

    protected static final int PROTECTION_TYPE = 37;
    protected static final int METRIC_VALUE_F_LENGTH = 4;
    protected static final int OVERALL_LENGTH = 7;
    protected static final int PARAMETER_127_LENGTH = 5;
    protected static final int PARAMETER_130_LENGTH = 2;
    protected static final int GUARANTEED_SERVICE_RSPEC = 130;
    protected static final int TOKEN_BUCKET_TSPEC = 127;
    protected static final int SHORT_SIZE = 2;
    protected static final int BYTE_SIZE = 1;
    protected static final int ONE = 1;
    protected static final int RESERVED = 1;
    protected static final int FLAGS_SIZE = 8;
    protected static final int IPV4_BYTES_LENGTH = 4;
    protected static final int IPV6_BYTES_LENGTH = 16;
    protected static final int IPV4_PREFIX_LENGTH = 8;
    protected static final int IPV6_PREFIX_LENGTH = 20;
    protected static final int AUTONOMUS_SYSTEM_LENGTH = 4;
    protected static final int SUBOBJECT_HEADER_LENGHT = 2;

    protected static void serializeAttributeHeader(final Integer valueLength, final short classNum, final short cType,
                                                   final ByteBuf byteAggregator) {
        byteAggregator.writeShort(valueLength);
        byteAggregator.writeByte(classNum);
        byteAggregator.writeByte(cType);
    }

    protected static void writeAttributeFilter(final AttributeFilter attributeFilter, final ByteBuf body) {
        writeUnsignedInt(attributeFilter != null ? attributeFilter.getValue() : null, body);
    }

    @VisibleForTesting
    @Override
    public RsvpTeObject parseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        if (byteBuf == null) {
            return null;
        }
        return localParseObject(byteBuf);
    }

    @VisibleForTesting
    @Override
    public void serializeObject(final RsvpTeObject rsvpTeObject, final ByteBuf output) {
        if (rsvpTeObject == null) {
            return;
        }
        localSerializeObject(rsvpTeObject, output);
    }

    protected abstract void localSerializeObject(final RsvpTeObject rsvpTeObject, final ByteBuf output);

    protected abstract RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException;
}
