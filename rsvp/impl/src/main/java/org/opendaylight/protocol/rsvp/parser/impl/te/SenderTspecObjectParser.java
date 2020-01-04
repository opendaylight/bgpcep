/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObjectBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class SenderTspecObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 12;
    public static final short CTYPE = 2;
    private static final int BODY_SIZE = 32;
    private static final int SERVICE_LENGTH = 6;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        //skip version number, reserved, Overall length
        byteBuf.skipBytes(Integer.BYTES);
        //skip Service header, reserved, Length of service
        byteBuf.skipBytes(Integer.BYTES);
        //skip Parameter ID, Parameter 127 flags, Parameter 127 length
        byteBuf.skipBytes(Integer.BYTES);

        return new TspecObjectBuilder()
                .setTokenBucketRate(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)))
                .setTokenBucketSize(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)))
                .setPeakDataRate(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)))
                .setMinimumPolicedUnit(ByteBufUtils.readUint32(byteBuf))
                .setMaximumPacketSize(ByteBufUtils.readUint32(byteBuf))
                .build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        checkArgument(teLspObject instanceof TspecObject, "SenderTspecObject is mandatory.");
        final TspecObject tspecObj = (TspecObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, CLASS_NUM, CTYPE, output);
        output.writeShort(0);
        output.writeShort(OVERALL_LENGTH);
        // FIXME: this is weird. Use explicit 1?
        output.writeByte(Byte.BYTES);
        output.writeByte(0);
        output.writeShort(SERVICE_LENGTH);
        output.writeByte(TOKEN_BUCKET_TSPEC);
        output.writeByte(0);
        output.writeShort(PARAMETER_127_LENGTH);
        writeFloat32(tspecObj.getTokenBucketRate(), output);
        writeFloat32(tspecObj.getTokenBucketSize(), output);
        writeFloat32(tspecObj.getPeakDataRate(), output);
        writeUnsignedInt(tspecObj.getMinimumPolicedUnit(), output);
        writeUnsignedInt(tspecObj.getMaximumPacketSize(), output);
    }
}
