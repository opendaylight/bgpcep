/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.linkstate.attribute.objects;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;
import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.TeLspObjectParser;
import org.opendaylight.protocol.bgp.parser.spi.TeLspObjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;

public abstract class AbstractBGPObjectParser implements TeLspObjectParser, TeLspObjectSerializer {

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
    protected static final int PROTECTION_TYPE = 37;
    protected static final int SLRG_TYPE = 34;
    protected static final int IPV4_PREFIX_LENGTH = 8;
    protected static final int IPV6_PREFIX_LENGTH = 20;
    protected static final int AUTONOMUS_SYSTEM_LENGTH = 12;

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
    public TeLspObject parseObject(final ByteBuf byteBuf) throws BGPParsingException {
        if (byteBuf == null) {
            return null;
        }
        return localParseObject(byteBuf);
    }

    @VisibleForTesting
    @Override
    public void serializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        if (teLspObject == null) {
            return;
        }
        localSerializeObject(teLspObject, output);
    }

    protected abstract void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output);

    protected abstract TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException;
}
