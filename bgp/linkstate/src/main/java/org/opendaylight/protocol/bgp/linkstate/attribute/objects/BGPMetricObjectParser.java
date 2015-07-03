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

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.metric.object.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.metric.object.MetricObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;

public final class BGPMetricObjectParser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 6;
    public static final short CTYPE = 1;
    private static final Integer BODY_SIZE = 8;
    private static final int BOUND = 7;
    private static final int COMPUTED = 6;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final MetricObjectBuilder builder = new MetricObjectBuilder();
        builder.setCType(CTYPE);
        builder.setClassNum(CLASS_NUM);
        MetricBuilder metricBuilder = new MetricBuilder();
        byteBuf.readShort();
        final BitArray flags = BitArray.valueOf(byteBuf.readByte());
        metricBuilder.setBound(flags.get(BOUND));
        metricBuilder.setComputed(flags.get(COMPUTED));
        metricBuilder.setMetricType(byteBuf.readUnsignedByte());
        metricBuilder.setValue(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        builder.setMetric(metricBuilder.build());
        return builder.build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof MetricObject, "BandwidthObject is mandatory.");
        final MetricObject metricContrainer = (MetricObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, metricContrainer.getClassNum(), metricContrainer.getCType(), output);
        final Metric metric = metricContrainer.getMetric();
        output.writeZero(SHORT_SIZE);
        final BitArray reflect = new BitArray(FLAGS_SIZE);
        reflect.set(BOUND, metric.isBound());
        reflect.set(COMPUTED, metric.isComputed());
        reflect.toByteBuf(output);
        output.writeByte(metric.getMetricType());
        writeFloat32(metric.getValue(), output);
    }
}
