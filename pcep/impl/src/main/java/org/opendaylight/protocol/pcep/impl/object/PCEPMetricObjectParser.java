/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBitSet;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;

/**
 * Parser for {@link Metric}
 */
public class PCEPMetricObjectParser implements ObjectParser, ObjectSerializer {

    public static final int CLASS = 6;

    public static final int TYPE = 1;

    /*
     * lengths of fields in bytes
     */
    private static final int FLAGS_F_LENGTH = 1;
    private static final int METRIC_VALUE_F_LENGTH = 4;

    /*
     * offsets of fields in bytes
     */
    private static final int RESERVED = 2;

    /*
     * flags offsets inside flags field in bits
     */
    private static final int C_FLAG_OFFSET = 6;
    private static final int B_FLAG_OFFSET = 7;

    private static final int SIZE = METRIC_VALUE_F_LENGTH + METRIC_VALUE_F_LENGTH;

    @Override
    public Metric parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() != SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: " + SIZE
                    + ".");
        }
        bytes.skipBytes(RESERVED);
        final byte[] flagBytes = { bytes.readByte() };
        final BitSet flags = ByteArray.bytesToBitSet(flagBytes);
        final MetricBuilder builder = new MetricBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        builder.setBound(flags.get(B_FLAG_OFFSET));
        builder.setComputed(flags.get(C_FLAG_OFFSET));
        builder.setMetricType(bytes.readUnsignedByte());
        builder.setValue(new Float32(ByteArray.readBytes(bytes, METRIC_VALUE_F_LENGTH)));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Metric, "Wrong instance of PCEPObject. Passed %s. Needed MetricObject.", object.getClass());
        final Metric mObj = (Metric) object;
        final ByteBuf body = Unpooled.buffer(SIZE);
        body.writeZero(RESERVED);
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        if (mObj.isComputed() != null) {
            flags.set(C_FLAG_OFFSET, mObj.isComputed());
        }
        if (mObj.isBound() != null) {
            flags.set(B_FLAG_OFFSET, mObj.isBound());
        }
        writeBitSet(flags, FLAGS_F_LENGTH, body);
        Preconditions.checkArgument(mObj.getMetricType() != null, "MetricType is mandatory.");
        writeUnsignedByte(mObj.getMetricType(), body);
        writeFloat32(mObj.getValue(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
