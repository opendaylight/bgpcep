/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.legacy.fast.reroute.object._case.LegacyFastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.fast.reroute.object.fast.reroute.object.legacy.fast.reroute.object._case.LegacyFastRerouteObjectBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class InformationalFastRerouteObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 205;
    public static final short CTYPE = 7;

    private static final int BODY_SIZE_C7 = 16;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final LegacyFastRerouteObjectBuilder builder = new LegacyFastRerouteObjectBuilder()
                .setSetupPriority(ByteBufUtils.readUint8(byteBuf))
                .setHoldPriority(ByteBufUtils.readUint8(byteBuf))
                .setHopLimit(ByteBufUtils.readUint8(byteBuf));

        //skip reserved
        byteBuf.skipBytes(Byte.BYTES);
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        builder.setIncludeAny(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)));
        builder.setExcludeAny(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        checkArgument(teLspObject instanceof LegacyFastRerouteObject, "FastRerouteObject is mandatory.");
        final LegacyFastRerouteObject fastRerouteObject = (LegacyFastRerouteObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE_C7, CLASS_NUM, CTYPE, byteAggregator);

        byteAggregator.writeByte(fastRerouteObject.getSetupPriority().toJava());
        byteAggregator.writeByte(fastRerouteObject.getHoldPriority().toJava());
        byteAggregator.writeByte(fastRerouteObject.getHopLimit().toJava());
        byteAggregator.writeByte(0);
        byteAggregator.writeBytes(Unpooled.wrappedBuffer(fastRerouteObject.getBandwidth().getValue()));
        writeAttributeFilter(fastRerouteObject.getIncludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getExcludeAny(), byteAggregator);
    }
}
