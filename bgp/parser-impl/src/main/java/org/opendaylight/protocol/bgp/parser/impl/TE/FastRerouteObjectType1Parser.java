/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.fast.reroute.object.FastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.fast.reroute.object.FastRerouteObjectBuilder;

public final class FastRerouteObjectType1Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 205;
    public static final short CTYPE = 1;
    private static final int BODY_SIZE_C1 = 20;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) {
        final FastRerouteObjectBuilder builder = new FastRerouteObjectBuilder();
        builder.setCType(CTYPE);
        builder.setSetupPriority(byteBuf.readUnsignedByte());
        builder.setHoldPriority(byteBuf.readUnsignedByte());
        builder.setHopLimit(byteBuf.readUnsignedByte());
        builder.setFlags(FastRerouteObject.Flags.forValue(byteBuf.readUnsignedByte()));
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        builder.setIncludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        builder.setExcludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        builder.setIncludeAll(new AttributeFilter(byteBuf.readUnsignedInt()));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof FastRerouteObject, "FastRerouteObject is mandatory.");
        final FastRerouteObject fastRerouteObject = (FastRerouteObject) teLspObject;

        serializeAttributeHeader(BODY_SIZE_C1, CLASS_NUM, CTYPE, byteAggregator);
        byteAggregator.writeByte(fastRerouteObject.getSetupPriority());
        byteAggregator.writeByte(fastRerouteObject.getHoldPriority());
        byteAggregator.writeByte(fastRerouteObject.getHopLimit());
        byteAggregator.writeByte(fastRerouteObject.getFlags().getIntValue());
        byteAggregator.writeBytes(Unpooled.wrappedBuffer(fastRerouteObject.getBandwidth().getValue()));
        writeAttributeFilter(fastRerouteObject.getIncludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getExcludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getIncludeAll(), byteAggregator);
    }
}
