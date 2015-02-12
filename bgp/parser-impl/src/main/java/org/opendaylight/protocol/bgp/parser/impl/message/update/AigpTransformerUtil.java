/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Aigp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AigpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.aigp.AigpTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.aigp.AigpTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class which provides basic transformation between byte arrays and Aigp attribute and vice versa.
 */
public class AigpTransformerUtil {

    private static final byte AIGP_TLV_TYPE = (byte)1;
    private static final short AIGP_TLV_SIZE = (short)11;
    private static final int TLV_SIZE_IN_BYTES = 2;

    private static final Logger LOG = LoggerFactory.getLogger(AigpTransformerUtil.class);

    /**
     * Transform AIGP attribute data from byte buffer representation into instance of Aigp class.
     * AIGP attribute will have list of only one AIGP TLV and it might contain one or more TLVs of
     * another type than AIGP TLV.
     *
     * @param buffer
     *          AIGP attribute data
     * @return
     *          instance of Aigp class or null if buffer is unreadable
     */
    public static Aigp byteArrayToAigp(ByteBuf buffer) {
        final AigpBuilder aigpBuilder = new AigpBuilder();

        if(!buffer.isReadable()) {
            return null;
        }
        final AigpTlv tlv = createAigpTlv(buffer);
        aigpBuilder.setAigpTlv(tlv);

        return aigpBuilder.build();
    }

    /**
     * Reads data from buffer until reaches TLV of type AIGP TLV.
     *
     * @param buffer
     *          TLVs in ByteBuf format
     * @return
     *          instance of AigpTlv class or null if buffer contains unknown TLV type
     */
    public static AigpTlv createAigpTlv(final ByteBuf buffer) {
        final AigpTlvBuilder tlvBuilder = new AigpTlvBuilder();
        AccumulatedIgpMetric accumulatedIgpMetric;
        int tlvType = buffer.readByte();
        buffer.skipBytes(TLV_SIZE_IN_BYTES);

        if (tlvType != AIGP_TLV_TYPE) {
            LOG.warn("AIGP attribute contains \"unknown\" TLV type {}.", tlvType);
            return null;
        }
        accumulatedIgpMetric = createAccumulatedIgpMetric(buffer.readLong());
        tlvBuilder.setMetric(accumulatedIgpMetric);

        return tlvBuilder.build();
    }

    /**
     * Creates instance of AIGP metric with concrete value.
     *
     * @param valueOfMetric
     *          concrete value of AIGP metric
     * @return
     *          instance of AIGP metric
     */
    public static AccumulatedIgpMetric createAccumulatedIgpMetric(final long valueOfMetric) {
        return new AccumulatedIgpMetric(BigInteger.valueOf(valueOfMetric));
    }

    /**
     * Transform AIGP attribute data from instance of Aigp class into byte buffer representation.
     *
     *
     * @param aigp
     *          instance of Aigp class
     * @return
     *          byte buffer representation or empty buffer if AIGP TLV is null
     */
    public static ByteBuf aigpToByteArray(final Aigp aigp) {
        final AigpTlv tlv = aigp.getAigpTlv();
        final ByteBuf value = Unpooled.buffer();

        if (tlv == null) {
            return value;
        }
        final AccumulatedIgpMetric metric = tlv.getMetric();
        final long metricValue = metric.getValue().longValue();

        value.writeByte(AIGP_TLV_TYPE);
        ByteBufWriteUtil.writeShort(AIGP_TLV_SIZE, value);
        ByteBufWriteUtil.writeLong(metricValue, value);

        return value;
    }
}
