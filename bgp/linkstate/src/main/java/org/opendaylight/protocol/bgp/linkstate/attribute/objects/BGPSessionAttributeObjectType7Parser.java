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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.session.attribute.object.SessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.session.attribute.object.SessionAttributeObjectBuilder;

public final class BGPSessionAttributeObjectType7Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 207;
    public static final short CTYPE = 7;
    private static final int LOCAL_PROTECTION = 7;
    private static final int LABEL_RECORDING = 6;
    private static final int SE_STYLE = 5;
    private static final int BODY_SIZE_C7 = 4;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final SessionAttributeObjectBuilder builder = new SessionAttributeObjectBuilder();
        builder.setCType(CTYPE);
        builder.setClassNum(CLASS_NUM);
        parseSessionAttributeObject(builder,byteBuf);
        return builder.build();
    }

    protected static void parseSessionAttributeObject(final SessionAttributeObjectBuilder builder, final ByteBuf buffer) {
        builder.setSetupPrio(buffer.readUnsignedByte());
        builder.setHoldingPrio(buffer.readUnsignedByte());
        final BitArray bs = BitArray.valueOf(buffer.readByte());
        builder.setFlags(new SessionAttributeObject.Flags(bs.get(LOCAL_PROTECTION), bs.get(LABEL_RECORDING), bs.get(SE_STYLE)));
        final short nameLenght = buffer.readUnsignedByte();
        builder.setNameLength(nameLenght);
        final ByteBuf auxBuf = buffer.readSlice(nameLenght);
        final String name = new String(ByteArray.readAllBytes(auxBuf), Charsets.US_ASCII);
        builder.setSessionName(name);
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof SessionAttributeObject, "SessionAttributeObject is mandatory.");

        final SessionAttributeObject sessionObject = (SessionAttributeObject) teLspObject;

        serializeAttributeHeader(BODY_SIZE_C7 + sessionObject.getNameLength(), sessionObject.getClassNum(),
            sessionObject.getCType(), output);
        serializeSessionObject(sessionObject, output);
    }

    protected static void serializeSessionObject(final SessionAttributeObject sessionObject, final ByteBuf output) {
        output.writeByte(sessionObject.getSetupPrio());
        output.writeByte(sessionObject.getHoldingPrio());
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(LOCAL_PROTECTION, sessionObject.getFlags().isLocalProtectionDesired());
        bs.set(LABEL_RECORDING, sessionObject.getFlags().isLabelRecordingDesired());
        bs.set(SE_STYLE, sessionObject.getFlags().isSeStyleDesired());
        bs.toByteBuf(output);
        output.writeByte(sessionObject.getNameLength());
        output.writeBytes(Unpooled.wrappedBuffer(Charsets.US_ASCII.encode(sessionObject.getSessionName())));
    }
}
