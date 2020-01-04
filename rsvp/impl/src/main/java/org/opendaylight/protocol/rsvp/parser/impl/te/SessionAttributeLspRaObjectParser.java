/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.session.attribute.object.with.resources.affinities._case.SessionAttributeObjectWithResourcesAffinities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.session.attribute.object.session.attribute.object.session.attribute.object.with.resources.affinities._case.SessionAttributeObjectWithResourcesAffinitiesBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class SessionAttributeLspRaObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 207;
    public static final short CTYPE = 1;
    private static final Short BODY_SIZE_C1 = 16;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final SessionAttributeObjectWithResourcesAffinitiesBuilder builder =
                new SessionAttributeObjectWithResourcesAffinitiesBuilder()
                    .setIncludeAny(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)))
                    .setExcludeAny(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)))
                    .setIncludeAll(new AttributeFilter(ByteBufUtils.readUint32(byteBuf)))
                    .setSetupPriority(ByteBufUtils.readUint8(byteBuf))
                    .setHoldPriority(ByteBufUtils.readUint8(byteBuf));

        final BitArray bs = BitArray.valueOf(byteBuf.readByte());
        builder.setLocalProtectionDesired(bs.get(SessionAttributeLspObjectParser.LOCAL_PROTECTION));
        builder.setLabelRecordingDesired(bs.get(SessionAttributeLspObjectParser.LABEL_RECORDING));
        builder.setSeStyleDesired(bs.get(SessionAttributeLspObjectParser.SE_STYLE));
        final short nameLenght = byteBuf.readUnsignedByte();
        final ByteBuf auxBuf = byteBuf.readSlice(nameLenght);
        final String name = new String(ByteArray.readAllBytes(auxBuf), StandardCharsets.US_ASCII);
        builder.setSessionName(name);
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        checkArgument(teLspObject instanceof SessionAttributeObjectWithResourcesAffinities,
            "SessionAttributeObject is mandatory.");

        final SessionAttributeObjectWithResourcesAffinities sessionObject =
            (SessionAttributeObjectWithResourcesAffinities) teLspObject;
        final ByteBuf sessionName = Unpooled.wrappedBuffer(StandardCharsets.US_ASCII.encode(sessionObject
            .getSessionName()));
        final int pad = SessionAttributeLspObjectParser.getPadding(sessionName.readableBytes());

        serializeAttributeHeader(BODY_SIZE_C1 + pad + sessionName.readableBytes(), CLASS_NUM, CTYPE, output);
        writeAttributeFilter(sessionObject.getIncludeAny(), output);
        writeAttributeFilter(sessionObject.getExcludeAny(), output);
        writeAttributeFilter(sessionObject.getIncludeAll(), output);
        output.writeByte(sessionObject.getSetupPriority().toJava());
        output.writeByte(sessionObject.getHoldPriority().toJava());
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(SessionAttributeLspObjectParser.LOCAL_PROTECTION, sessionObject.isLocalProtectionDesired());
        bs.set(SessionAttributeLspObjectParser.LABEL_RECORDING, sessionObject.isLabelRecordingDesired());
        bs.set(SessionAttributeLspObjectParser.SE_STYLE, sessionObject.isSeStyleDesired());
        bs.toByteBuf(output);
        output.writeByte(sessionName.readableBytes());
        output.writeBytes(Unpooled.wrappedBuffer(StandardCharsets.US_ASCII.encode(sessionObject.getSessionName())));
        output.writeZero(pad);
    }
}
