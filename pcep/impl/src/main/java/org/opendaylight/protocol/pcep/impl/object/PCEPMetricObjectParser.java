/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;

/**
 * Parser for {@link Metric}
 */
public class PCEPMetricObjectParser extends AbstractObjectWithTlvsParser<MetricBuilder> {

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

    private static final int SIZE = 4 + METRIC_VALUE_F_LENGTH;

    public PCEPMetricObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Metric parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() != SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: " + SIZE
                    + ".");
        }
        bytes.readerIndex(bytes.readerIndex() + RESERVED);
        final byte[] flagBytes = { bytes.readByte() };
        final BitSet flags = ByteArray.bytesToBitSet(flagBytes);
        final MetricBuilder builder = new MetricBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        builder.setBound(flags.get(B_FLAG_OFFSET));
        builder.setComputed(flags.get(C_FLAG_OFFSET));
        builder.setMetricType((short) UnsignedBytes.toInt(bytes.readByte()));
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
        body.writeBytes(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH));
        body.writeByte(mObj.getMetricType());
        body.writeBytes(mObj.getValue().getValue());
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
