/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.fast.reroute.object.FastRerouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.fast.reroute.object.FastRerouteObjectBuilder;
import parser.spi.RSVPParsingException;

public final class FastRerouteObjectType7Parser extends AbstractBGPObjectParser {

    public static final short CLASS_NUM = 205;
    public static final short CTYPE = 7;
    private static final int BODY_SIZE_C7 = 16;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final FastRerouteObjectBuilder builder = new FastRerouteObjectBuilder();
        builder.setCType(CTYPE);
        builder.setSetupPriority(byteBuf.readUnsignedByte());
        builder.setHoldPriority(byteBuf.readUnsignedByte());
        builder.setHopLimit(byteBuf.readUnsignedByte());
        //skip reserved
        byteBuf.readUnsignedByte();
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        builder.setIncludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        builder.setExcludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof FastRerouteObject, "FastRerouteObject is mandatory.");
        final FastRerouteObject fastRerouteObject = (FastRerouteObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE_C7, CLASS_NUM, CTYPE, byteAggregator);

        byteAggregator.writeByte(fastRerouteObject.getSetupPriority());
        byteAggregator.writeByte(fastRerouteObject.getHoldPriority());
        byteAggregator.writeByte(fastRerouteObject.getHopLimit());
        byteAggregator.writeZero(BYTE_SIZE);
        byteAggregator.writeBytes(Unpooled.wrappedBuffer(fastRerouteObject.getBandwidth().getValue()));
        writeAttributeFilter(fastRerouteObject.getIncludeAny(), byteAggregator);
        writeAttributeFilter(fastRerouteObject.getExcludeAny(), byteAggregator);
    }
}
