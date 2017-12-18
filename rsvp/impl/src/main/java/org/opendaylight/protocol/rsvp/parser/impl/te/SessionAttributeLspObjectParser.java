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
import java.nio.charset.StandardCharsets;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.BasicSessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.BasicSessionAttributeObjectBuilder;

public final class SessionAttributeLspObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 207;
    public static final short CTYPE = 7;
    static final int LOCAL_PROTECTION = 7;
    static final int LABEL_RECORDING = 6;
    static final int SE_STYLE = 5;
    private static final int BODY_SIZE_C7 = 4;
    private static final int PADDING = 4;

    static int getPadding(final int length) {
        return (PADDING - (length % PADDING)) % PADDING;
    }

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
        final ByteBuf auxBuf = byteBuf.readSlice(nameLenght);
        final String name = new String(ByteArray.readAllBytes(auxBuf), StandardCharsets.US_ASCII);
        builder.setSessionName(name);
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BasicSessionAttributeObject,
            "SessionAttributeObject is mandatory.");

        final BasicSessionAttributeObject sessionObject = (BasicSessionAttributeObject) teLspObject;
        final ByteBuf sessionName = Unpooled.wrappedBuffer(StandardCharsets.US_ASCII.encode(sessionObject
            .getSessionName()));
        final int pad = getPadding(sessionName.readableBytes());
        serializeAttributeHeader(BODY_SIZE_C7 + pad + sessionName.readableBytes(), CLASS_NUM, CTYPE, output);
        output.writeByte(sessionObject.getSetupPriority());
        output.writeByte(sessionObject.getHoldPriority());
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(LOCAL_PROTECTION, sessionObject.isLocalProtectionDesired());
        bs.set(LABEL_RECORDING, sessionObject.isLabelRecordingDesired());
        bs.set(SE_STYLE, sessionObject.isSeStyleDesired());
        bs.toByteBuf(output);
        output.writeByte(sessionName.readableBytes());
        output.writeBytes(sessionName);
        output.writeZero(pad);
    }
}
