/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.impl.subobject.RRO;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LinkFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.protection.subobject.ProtectionSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.protection.subobject.ProtectionSubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.ProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.ProtectionCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.spi.RROSubobjectParser;
import parser.spi.RROSubobjectSerializer;
import parser.spi.RSVPParsingException;

/**
 * Parser for {@link ProtectionCase}
 */
public class SRROProtectionSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 37;
    private static final Logger LOG = LoggerFactory.getLogger(SRROProtectionSubobjectParser.class);

    private static final short PROTECTION_SUBOBJECT_TYPE_1 = 1;
    private static final short PROTECTION_SUBOBJECT_TYPE_2 = 2;
    private static final int SECONDARY = 0;
    private static final int PROTECTING = 1;
    private static final int NOTIFICATION = 2;
    private static final int OPERATIONAL = 3;
    private static final int IN_PLACE = 0;
    private static final int REQUIRED = 1;
    private static final int FLAGS_SIZE = 8;
    private static final int SHORT_SIZE = 2;
    private static final int BYTE_SIZE = 1;
    private static final int CONTENT_LENGTH_C2 = 8;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();

        //skip reserved
        buffer.readByte();
        final short cType = buffer.readUnsignedByte();
        switch (cType) {
        case PROTECTION_SUBOBJECT_TYPE_1:
            builder.setSubobjectType(new ProtectionCaseBuilder().setProtectionSubobject
                (parseCommonProtectionBodyType1(buffer)).build());
            break;
        case PROTECTION_SUBOBJECT_TYPE_2:
            builder.setSubobjectType(new ProtectionCaseBuilder().setProtectionSubobject
                (parseCommonProtectionBodyType2(buffer)).build());
            break;
        default:
            LOG.warn("Secondary Record Route Protection Subobject cType {} not supported", cType);
            break;
        }
        return builder.build();
    }

    private ProtectionSubobject parseCommonProtectionBodyType2(final ByteBuf byteBuf) throws RSVPParsingException {
        if (byteBuf.readableBytes() != CONTENT_LENGTH_C2) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + byteBuf.readableBytes() + "; Expected: " + CONTENT_LENGTH_C2 + ".");
        }
        ProtectionSubobjectBuilder sub = new ProtectionSubobjectBuilder();
        final BitArray bitArray1 = BitArray.valueOf(byteBuf.readByte());
        sub.setCType(PROTECTION_SUBOBJECT_TYPE_2);
        sub.setSecondary(bitArray1.get(SECONDARY));
        sub.setProtecting(bitArray1.get(PROTECTING));
        sub.setNotification(bitArray1.get(NOTIFICATION));
        sub.setOperational(bitArray1.get(OPERATIONAL));

        final int lspFlags = byteBuf.readByte();
        sub.setLspFlag(LspFlag.forValue(lspFlags)).build();
        //Skip Reserved
        byteBuf.readByte();
        final int linkFlags = byteBuf.readByte();
        sub.setLinkFlags(LinkFlags.forValue(linkFlags));

        final BitArray bitArray2 = BitArray.valueOf(byteBuf.readByte());
        sub.setInPlace(bitArray2.get(IN_PLACE));
        sub.setRequired(bitArray2.get(REQUIRED));

        final int segFlags = byteBuf.readByte();
        sub.setSegFlag(LspFlag.forValue(segFlags));
        byteBuf.readShort();
        return sub.build();
    }

    private ProtectionSubobject parseCommonProtectionBodyType1(final ByteBuf byteBuf) {
        BitArray bitArray = BitArray.valueOf(byteBuf.readByte());
        ProtectionSubobjectBuilder sub = new ProtectionSubobjectBuilder();
        sub.setCType(PROTECTION_SUBOBJECT_TYPE_1);
        sub.setSecondary(bitArray.get(SECONDARY));
        //Skip Reserved
        byteBuf.readShort();
        int linkFlags = byteBuf.readByte();
        sub.setLinkFlags(LinkFlags.forValue(linkFlags));
        return sub.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof ProtectionCase, "Unknown subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final ProtectionSubobject protObj = ((ProtectionCase) subobject.getSubobjectType()).getProtectionSubobject();
        final ByteBuf body = Unpooled.buffer();

        body.writeZero(BYTE_SIZE);
        final Short cType = protObj.getCType();
        writeUnsignedByte(cType, body);
        switch (cType) {
        case PROTECTION_SUBOBJECT_TYPE_1:
            serializeBodyType1(protObj, body);
            break;
        case PROTECTION_SUBOBJECT_TYPE_2:
            serializeBodyType2(protObj, body);
            break;
        }
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }

    private void serializeBodyType1(final ProtectionSubobject protObj, final ByteBuf output) {
        final BitArray bitArray = new BitArray(FLAGS_SIZE);
        bitArray.set(SECONDARY, protObj.isSecondary());
        bitArray.toByteBuf(output);
        output.writeZero(SHORT_SIZE);
        output.writeByte(protObj.getLinkFlags().getIntValue());
    }

    private void serializeBodyType2(final ProtectionSubobject protObj, final ByteBuf output) {
        BitArray bitArray = new BitArray(FLAGS_SIZE);

        bitArray.set(SECONDARY, protObj.isSecondary());
        bitArray.set(PROTECTING, protObj.isProtecting());
        bitArray.set(NOTIFICATION, protObj.isNotification());
        bitArray.set(OPERATIONAL, protObj.isOperational());
        bitArray.toByteBuf(output);
        output.writeByte(protObj.getLspFlag().getIntValue());
        output.writeZero(BYTE_SIZE);
        output.writeByte(protObj.getLinkFlags().getIntValue());
        bitArray = new BitArray(FLAGS_SIZE);
        bitArray.set(IN_PLACE, protObj.isInPlace());
        bitArray.set(REQUIRED, protObj.isRequired());
        bitArray.toByteBuf(output);
        output.writeByte(protObj.getSegFlag().getIntValue());
        output.writeZero(SHORT_SIZE);
    }
}
