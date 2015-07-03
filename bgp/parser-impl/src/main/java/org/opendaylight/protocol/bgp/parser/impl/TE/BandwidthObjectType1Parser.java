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
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.bandwidth.object.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.bandwidth.object.BandwidthObjectBuilder;

public final class BandwidthObjectType1Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 5;
    public static final short CTYPE = 1;
    private static final Integer BODY_SIZE = 4;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final BandwidthObjectBuilder builder = new BandwidthObjectBuilder();
        // builder.setCType(CTYPE);
        // builder.setClassNum(CLASS_NUM);
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BandwidthObject, "BandwidthObject is mandatory.");
        final BandwidthObject bandObject = (BandwidthObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, CLASS_NUM, CTYPE, output);
        final Bandwidth band = bandObject.getBandwidth();
        output.writeBytes(Unpooled.wrappedBuffer(band.getValue()));
    }
}
