/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.te;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.metric.object.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.metric.object.MetricObjectBuilder;

public final class MetricObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 6;
    public static final short CTYPE = 1;
    private static final Integer BODY_SIZE = 8;
    private static final int BOUND = 7;
    private static final int COMPUTED = 6;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final MetricObjectBuilder builder = new MetricObjectBuilder();
        byteBuf.skipBytes(ByteBufWriteUtil.SHORT_BYTES_LENGTH);
        final BitArray flags = BitArray.valueOf(byteBuf.readByte());
        builder.setBound(flags.get(BOUND));
        builder.setComputed(flags.get(COMPUTED));
        builder.setMetricType(byteBuf.readUnsignedByte());
        builder.setValue(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof MetricObject, "BandwidthObject is mandatory.");
        final MetricObject metric = (MetricObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, CLASS_NUM, CTYPE, output);
        output.writeZero(ByteBufWriteUtil.SHORT_BYTES_LENGTH);
        final BitArray reflect = new BitArray(FLAGS_SIZE);
        reflect.set(BOUND, metric.isBound());
        reflect.set(COMPUTED, metric.isComputed());
        reflect.toByteBuf(output);
        output.writeByte(metric.getMetricType());
        writeFloat32(metric.getValue(), output);
    }
}
