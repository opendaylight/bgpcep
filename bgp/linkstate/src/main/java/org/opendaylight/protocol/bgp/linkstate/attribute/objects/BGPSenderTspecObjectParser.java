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
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.sender.tspec.object.SenderTspecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.sender.tspec.object.SenderTspecObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

public class BGPSenderTspecObjectParser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 12;
    public static final short CTYPE = 2;
    private static final int BODY_SIZE = 32;
    private static final int SERVICE_LENGHT = 6;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final SenderTspecObjectBuilder builder = new SenderTspecObjectBuilder();
        builder.setCType(CTYPE);
        builder.setClassNum(CLASS_NUM);
        //skip version number, reserved, Overall length
        byteBuf.readInt();
        //skip Service header, reserved, Length of service
        byteBuf.readInt();
        //skip Parameter ID, Parameter 127 flags, Parameter 127 length
        byteBuf.readInt();

        builder.setTokenBucketRate(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        builder.setTokenBucketSize(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        builder.setPeakDataRate(new Float32(ByteArray.readBytes(byteBuf, METRIC_VALUE_F_LENGTH)));
        builder.setMinimumPolicedUnit(byteBuf.readUnsignedInt());
        builder.setMaximumPacketSize(byteBuf.readUnsignedInt());
        return builder.build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof SenderTspecObject, "SenderTspecObject is mandatory.");
        final SenderTspecObject tspecObj = (SenderTspecObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, tspecObj.getClassNum(), tspecObj.getCType(), output);
        output.writeZero(SHORT_SIZE);
        output.writeShort(OVERALL_LENGTH);
        output.writeByte(ONE);
        output.writeZero(RESERVED);
        output.writeShort(SERVICE_LENGHT);
        output.writeByte(TOKEN_BUCKET_TSPEC);
        output.writeZero(RESERVED);
        output.writeShort(PARAMETER_127_LENGTH);
        writeFloat32(tspecObj.getTokenBucketRate(), output);
        writeFloat32(tspecObj.getTokenBucketSize(), output);
        writeFloat32(tspecObj.getPeakDataRate(), output);
        writeUnsignedInt(tspecObj.getMinimumPolicedUnit(), output);
        writeUnsignedInt(tspecObj.getMaximumPacketSize(), output);
    }
}
