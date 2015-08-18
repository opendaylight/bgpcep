/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.te;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ServiceNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.flow.spec.object.FlowSpecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.flow.spec.object.FlowSpecObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.tspec.object.TspecObjectBuilder;

public final class FlowSpecObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 9;
    public static final short CTYPE = 2;
    private static final Integer BODY_SIZE_CONTROLLED = 32;
    private static final Integer BODY_SIZE_REQUESTING = 44;
    private static final Integer CONTROLLER_OVERALL_LENGHT = 7;
    private static final Integer REQUESTING_OVERALL_LENGTH = 10;
    private static final int SERVICE_LENGHT = 9;
    private static final int CONTROLLED_LENGHT = 6;

    @Override
    final protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final FlowSpecObjectBuilder builder = new FlowSpecObjectBuilder();
        //skip version number, reserved, overall length
        byteBuf.skipBytes(ByteBufWriteUtil.INT_BYTES_LENGTH);
        builder.setServiceHeader(ServiceNumber.forValue(byteBuf.readUnsignedByte()));
        //skip reserved
        byteBuf.skipBytes(ByteBufWriteUtil.ONE_BYTE_LENGTH);
        //skip Length of controlled-load data
        byteBuf.skipBytes(ByteBufWriteUtil.SHORT_BYTES_LENGTH);
        //skip parameter ID 127 and 127 flags
        byteBuf.skipBytes(ByteBufWriteUtil.INT_BYTES_LENGTH);
        final TspecObjectBuilder tBuilder = new TspecObjectBuilder();
        tBuilder.setTokenBucketRate(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        tBuilder.setTokenBucketSize(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        tBuilder.setPeakDataRate(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        tBuilder.setMinimumPolicedUnit(byteBuf.readUnsignedInt());
        tBuilder.setMaximumPacketSize(byteBuf.readUnsignedInt());
        builder.setTspecObject(tBuilder.build());
        if (builder.getServiceHeader().getIntValue() == 2) {
            //skip parameter ID 130, flags, lenght
            byteBuf.skipBytes(ByteBufWriteUtil.INT_BYTES_LENGTH);
            builder.setRate(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
            builder.setSlackTerm(byteBuf.readUnsignedInt());
        }
        return builder.build();
    }

    @Override
    final public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof FlowSpecObject, "SenderTspecObject is mandatory.");
        final FlowSpecObject flowObj = (FlowSpecObject) teLspObject;
        final int sHeader = flowObj.getServiceHeader().getIntValue();
        if (sHeader == 2) {
            serializeAttributeHeader(BODY_SIZE_REQUESTING, CLASS_NUM, CTYPE, output);
            output.writeZero(ByteBufWriteUtil.SHORT_BYTES_LENGTH);
            output.writeShort(REQUESTING_OVERALL_LENGTH);
        } else {
            serializeAttributeHeader(BODY_SIZE_CONTROLLED, CLASS_NUM, CTYPE, output);
            output.writeZero(ByteBufWriteUtil.SHORT_BYTES_LENGTH);
            output.writeShort(CONTROLLER_OVERALL_LENGHT);
        }
        output.writeByte(sHeader);
        output.writeZero(ByteBufWriteUtil.ONE_BYTE_LENGTH);
        if (sHeader == 2) {
            output.writeShort(SERVICE_LENGHT);
        } else {
            output.writeShort(CONTROLLED_LENGHT);
        }

        output.writeByte(TOKEN_BUCKET_TSPEC);
        output.writeZero(ByteBufWriteUtil.ONE_BYTE_LENGTH);
        output.writeShort(PARAMETER_127_LENGTH);
        final TspecObject tSpec = flowObj.getTspecObject();
        writeFloat32(tSpec.getTokenBucketRate(), output);
        writeFloat32(tSpec.getTokenBucketSize(), output);
        writeFloat32(tSpec.getPeakDataRate(), output);
        writeUnsignedInt(tSpec.getMinimumPolicedUnit(), output);
        writeUnsignedInt(tSpec.getMaximumPacketSize(), output);

        if (sHeader != 2) {
            return;
        }
        output.writeByte(GUARANTEED_SERVICE_RSPEC);
        output.writeZero(ByteBufWriteUtil.ONE_BYTE_LENGTH);
        output.writeShort(PARAMETER_130_LENGTH);
        writeFloat32(flowObj.getRate(), output);
        writeUnsignedInt(flowObj.getSlackTerm(), output);
    }
}
