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
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.fast.reroute.object.FastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.fast.reroute.object.FastRerouteObject.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.fast.reroute.object.FastRerouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;

public final class BGPFastRerouteObjectType1Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 205;
    public static final short CTYPE = 1;
    private static final int ONE_TO_ONE_BACKUP = 7;
    private static final int FACILITY_BACKUP = 6;
    private static final int BODY_SIZE_C1 = 20;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) {
        final FastRerouteObjectBuilder fastBuilder = new FastRerouteObjectBuilder();
        fastBuilder.setClassNum(CLASS_NUM);
        fastBuilder.setCType(CTYPE);
        fastBuilder.setSetupPrio(byteBuf.readUnsignedByte());
        fastBuilder.setHoldingPrio(byteBuf.readUnsignedByte());
        fastBuilder.setHopLimit(byteBuf.readUnsignedByte());
        final BitArray bs =  BitArray.valueOf(byteBuf, FLAGS_SIZE);
        fastBuilder.setFlags(new Flags(bs.get(FACILITY_BACKUP), bs.get(ONE_TO_ONE_BACKUP)));
        fastBuilder.setBandwidth(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        fastBuilder.setIncludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        fastBuilder.setExcludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        fastBuilder.setIncludeAll(new AttributeFilter(byteBuf.readUnsignedInt()));
        return fastBuilder.build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof FastRerouteObject, "FastRerouteObject is mandatory.");
        final FastRerouteObject fastRerouteObject = (FastRerouteObject) teLspObject;

        serializeAttributeHeader(BODY_SIZE_C1, fastRerouteObject.getClassNum(), fastRerouteObject.getCType(), byteAggregator);
        byteAggregator.writeByte(fastRerouteObject.getSetupPrio());
        byteAggregator.writeByte(fastRerouteObject.getHoldingPrio());
        byteAggregator.writeByte(fastRerouteObject.getHopLimit());
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(FACILITY_BACKUP, fastRerouteObject.getFlags().isFacilityBackupDesired());
        bs.set(ONE_TO_ONE_BACKUP, fastRerouteObject.getFlags().isOneToOneBackupDesired());
        bs.toByteBuf(byteAggregator);
        writeFloat32(fastRerouteObject.getBandwidth(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getIncludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getExcludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getIncludeAll(), byteAggregator);
    }
}
