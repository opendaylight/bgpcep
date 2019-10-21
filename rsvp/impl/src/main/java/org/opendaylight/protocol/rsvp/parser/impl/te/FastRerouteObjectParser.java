/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.FastRerouteFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.basic.fast.reroute.object._case.BasicFastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.basic.fast.reroute.object._case.BasicFastRerouteObjectBuilder;

public final class FastRerouteObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 205;
    public static final short CTYPE = 1;
    private static final int BODY_SIZE_C1 = 20;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) {
        final BasicFastRerouteObjectBuilder builder = new BasicFastRerouteObjectBuilder()
<<<<<<< HEAD   (2d0a37 Rework prependAS())
                .setSetupPriority(ByteBufUtils.readUint8(byteBuf))
                .setHoldPriority(ByteBufUtils.readUint8(byteBuf))
                .setHopLimit(ByteBufUtils.readUint8(byteBuf))
=======
                .setSetupPriority(byteBuf.readUnsignedByte())
                .setHoldPriority(byteBuf.readUnsignedByte())
                .setHopLimit(byteBuf.readUnsignedByte())
>>>>>>> CHANGE (e119dd Fix rsvp.yang)
                .setFlags(FastRerouteFlags.forValue(byteBuf.readUnsignedByte()));
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
<<<<<<< HEAD   (2d0a37 Rework prependAS())
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        builder.setIncludeAny(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)));
        builder.setExcludeAny(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)));
        builder.setIncludeAll(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)));
        return builder.build();
=======

        return builder
                .setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)))
                .setIncludeAny(new AttributeFilter(byteBuf.readUnsignedInt()))
                .setExcludeAny(new AttributeFilter(byteBuf.readUnsignedInt()))
                .setIncludeAll(new AttributeFilter(byteBuf.readUnsignedInt()))
                .build();
>>>>>>> CHANGE (e119dd Fix rsvp.yang)
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof BasicFastRerouteObject, "FastRerouteObject is mandatory.");
        final BasicFastRerouteObject fastRerouteObject = (BasicFastRerouteObject) teLspObject;

        serializeAttributeHeader(BODY_SIZE_C1, CLASS_NUM, CTYPE, byteAggregator);
        byteAggregator.writeByte(fastRerouteObject.getSetupPriority().toJava());
        byteAggregator.writeByte(fastRerouteObject.getHoldPriority().toJava());
        byteAggregator.writeByte(fastRerouteObject.getHopLimit().toJava());
        byteAggregator.writeByte(fastRerouteObject.getFlags().getIntValue());
        byteAggregator.writeBytes(Unpooled.wrappedBuffer(fastRerouteObject.getBandwidth().getValue()));
        writeAttributeFilter(fastRerouteObject.getIncludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getExcludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getIncludeAll(), byteAggregator);
    }
}
