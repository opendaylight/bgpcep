/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package parser.impl.TE;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.BasicSessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.BasicSessionAttributeObjectBuilder;
import parser.spi.RSVPParsingException;

public final class SessionAttributeObjectType7Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 207;
    public static final short CTYPE = 7;
    protected static final int LOCAL_PROTECTION = 7;
    protected static final int LABEL_RECORDING = 6;
    protected static final int SE_STYLE = 5;
    private static final int BODY_SIZE_C7 = 4;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final BasicSessionAttributeObjectBuilder builder = new BasicSessionAttributeObjectBuilder();
        builder.setSetupPriority(byteBuf.readUnsignedByte());
        builder.setHoldPriority(byteBuf.readUnsignedByte());
        final BitArray bs = BitArray.valueOf(byteBuf.readByte());
        builder.setLocalProtectionDesired(bs.get(LOCAL_PROTECTION));
        builder.setLabelRecordingDesired(bs.get(LABEL_RECORDING));
        builder.setSeStyleDesired(bs.get(SE_STYLE));
        final short nameLenght = byteBuf.readUnsignedByte();
        builder.setNameLength(nameLenght);
        final ByteBuf auxBuf = byteBuf.readSlice(nameLenght);
        final String name = new String(ByteArray.readAllBytes(auxBuf), Charsets.US_ASCII);
        builder.setSessionName(name);
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BasicSessionAttributeObject, "SessionAttributeObject is mandatory.");

        final BasicSessionAttributeObject sessionObject = (BasicSessionAttributeObject) teLspObject;

        serializeAttributeHeader(BODY_SIZE_C7 + sessionObject.getNameLength(), CLASS_NUM, CTYPE, output);
        output.writeByte(sessionObject.getSetupPriority());
        output.writeByte(sessionObject.getHoldPriority());
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(LOCAL_PROTECTION, sessionObject.isLocalProtectionDesired());
        bs.set(LABEL_RECORDING, sessionObject.isLabelRecordingDesired());
        bs.set(SE_STYLE, sessionObject.isSeStyleDesired());
        bs.toByteBuf(output);
        output.writeByte(sessionObject.getNameLength());
        output.writeBytes(Unpooled.wrappedBuffer(Charsets.US_ASCII.encode(sessionObject.getSessionName())));
    }
}
