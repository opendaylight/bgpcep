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
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.bandwidth.object.ReoptimizationBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.bandwidth.object.bandwidth.object.ReoptimizationBandwidthObjectBuilder;

public final class ReoptimizationBandwidthObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 5;
    public static final short CTYPE = 2;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final ReoptimizationBandwidthObjectBuilder builder = new ReoptimizationBandwidthObjectBuilder();
        final ByteBuf v = byteBuf.readSlice(METRIC_VALUE_F_LENGTH);
        builder.setBandwidth(new Bandwidth(ByteArray.readAllBytes(v)));
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof ReoptimizationBandwidthObject, "BandwidthObject is mandatory.");
        final ReoptimizationBandwidthObject bandObject = (ReoptimizationBandwidthObject) teLspObject;
        serializeAttributeHeader(BandwidthObjectParser.BODY_SIZE, CLASS_NUM, CTYPE, output);
        final Bandwidth band = bandObject.getBandwidth();
        output.writeBytes(Unpooled.wrappedBuffer(band.getValue()));
    }
}