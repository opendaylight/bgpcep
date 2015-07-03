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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.session.attribute.object.SessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.session.attribute.object.SessionAttributeObjectBuilder;

public final class SessionAttributeObjectType7Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 207;
    public static final short CTYPE = 7;
    private static final int LOCAL_PROTECTION = 7;
    private static final int LABEL_RECORDING = 6;
    private static final int SE_STYLE = 5;
    private static final int BODY_SIZE_C7 = 4;

    protected static void parseSessionAttributeObject(final SessionAttributeObjectBuilder builder, final ByteBuf buffer) {
        builder.setSetupPriority(buffer.readUnsignedByte());
        builder.setHoldPriority(buffer.readUnsignedByte());
        final BitArray bs = BitArray.valueOf(buffer.readByte());
        builder.setLocalProtectionDesired(bs.get(LOCAL_PROTECTION));
        builder.setLabelRecordingDesired(bs.get(LABEL_RECORDING));
        builder.setSeStyleDesired(bs.get(SE_STYLE));
        final short nameLenght = buffer.readUnsignedByte();
        builder.setNameLength(nameLenght);
        final ByteBuf auxBuf = buffer.readSlice(nameLenght);
        final String name = new String(ByteArray.readAllBytes(auxBuf), Charsets.US_ASCII);
        builder.setSessionName(name);
    }

    protected static void serializeSessionObject(final SessionAttributeObject sessionObject, final ByteBuf output) {
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

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final SessionAttributeObjectBuilder builder = new SessionAttributeObjectBuilder();
        builder.setCType(CTYPE);
        parseSessionAttributeObject(builder, byteBuf);
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof SessionAttributeObject, "SessionAttributeObject is mandatory.");

        final SessionAttributeObject sessionObject = (SessionAttributeObject) teLspObject;

        serializeAttributeHeader(BODY_SIZE_C7 + sessionObject.getNameLength(), CLASS_NUM, CTYPE, output);
        serializeSessionObject(sessionObject, output);
    }
}
