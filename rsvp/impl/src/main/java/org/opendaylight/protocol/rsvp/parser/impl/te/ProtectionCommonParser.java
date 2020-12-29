/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LinkFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.subobject.ProtectionSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.subobject.ProtectionSubobjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectionCommonParser {

    protected static final short PROTECTION_SUBOBJECT_TYPE_1 = 1;
    protected static final short PROTECTION_SUBOBJECT_TYPE_2 = 2;
    protected static final int CONTENT_LENGTH_C2 = 8;
    private static final int SECONDARY = 0;
    private static final int PROTECTING = 1;
    private static final int NOTIFICATION = 2;
    private static final int OPERATIONAL = 3;
    private static final int IN_PLACE = 0;
    private static final int REQUIRED = 1;
    private static final int FLAGS_SIZE = 8;
    private static final Logger LOG = LoggerFactory.getLogger(ProtectionCommonParser.class);

    protected ProtectionCommonParser() {

    }

    protected static void serializeBodyType1(final ProtectionSubobject protObj, final ByteBuf output) {
        final BitArray flagBitArray = new BitArray(FLAGS_SIZE);
        flagBitArray.set(SECONDARY, protObj.getSecondary());
        flagBitArray.toByteBuf(output);
        output.writeShort(0);
        output.writeByte(protObj.getLinkFlags().getIntValue());
    }

    protected static void serializeBodyType2(final ProtectionSubobject protObj, final ByteBuf output) {
        final BitArray flagBitArray = new BitArray(FLAGS_SIZE);
        flagBitArray.set(SECONDARY, protObj.getSecondary());
        flagBitArray.set(PROTECTING, protObj.getProtecting());
        flagBitArray.set(NOTIFICATION, protObj.getNotification());
        flagBitArray.set(OPERATIONAL, protObj.getOperational());
        flagBitArray.toByteBuf(output);
        output.writeByte(protObj.getLspFlag().getIntValue());
        output.writeByte(0);
        output.writeByte(protObj.getLinkFlags().getIntValue());
        final BitArray flagInPlaceBitArray = new BitArray(FLAGS_SIZE);
        flagInPlaceBitArray.set(IN_PLACE, protObj.getInPlace());
        flagInPlaceBitArray.set(REQUIRED, protObj.getRequired());
        flagInPlaceBitArray.toByteBuf(output);
        output.writeByte(protObj.getSegFlag().getIntValue());
        output.writeShort(0);
    }

    protected static ProtectionSubobject parseCommonProtectionBodyType2(final ByteBuf byteBuf) throws
        RSVPParsingException {
        if (byteBuf.readableBytes() != CONTENT_LENGTH_C2) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + byteBuf.readableBytes() + "; "
                + "Expected: " + CONTENT_LENGTH_C2 + ".");
        }
        final ProtectionSubobjectBuilder sub = new ProtectionSubobjectBuilder();
        final BitArray protectionFlag = BitArray.valueOf(byteBuf.readByte());
        sub.setSecondary(protectionFlag.get(SECONDARY));
        sub.setProtecting(protectionFlag.get(PROTECTING));
        sub.setNotification(protectionFlag.get(NOTIFICATION));
        sub.setOperational(protectionFlag.get(OPERATIONAL));

        final int lspFlags = byteBuf.readByte();
        sub.setLspFlag(LspFlag.forValue(lspFlags)).build();
        //Skip Reserved
        byteBuf.skipBytes(Byte.BYTES);
        final int linkFlags = byteBuf.readByte();
        sub.setLinkFlags(LinkFlags.forValue(linkFlags));

        final BitArray bitArray2 = BitArray.valueOf(byteBuf.readByte());
        sub.setInPlace(bitArray2.get(IN_PLACE));
        sub.setRequired(bitArray2.get(REQUIRED));

        final int segFlags = byteBuf.readByte();
        sub.setSegFlag(LspFlag.forValue(segFlags));
        byteBuf.skipBytes(Short.BYTES);
        return sub.build();
    }

    protected static ProtectionSubobject parseCommonProtectionBodyType1(final ByteBuf byteBuf) {
        final BitArray bitArray = BitArray.valueOf(byteBuf.readByte());
        final ProtectionSubobjectBuilder sub = new ProtectionSubobjectBuilder();
        sub.setSecondary(bitArray.get(SECONDARY));
        //Skip Reserved
        byteBuf.skipBytes(Short.BYTES);
        final int linkFlags = byteBuf.readByte();
        sub.setLinkFlags(LinkFlags.forValue(linkFlags));
        return sub.build();
    }

    protected static void serializeBody(final short ctype, final ProtectionSubobject protObj,
        final ByteBuf output) {
        output.writeByte(0);
        output.writeByte(ctype);
        switch (ctype) {
            case PROTECTION_SUBOBJECT_TYPE_1:
                serializeBodyType1(protObj, output);
                break;
            case PROTECTION_SUBOBJECT_TYPE_2:
                serializeBodyType2(protObj, output);
                break;
            default:
                LOG.warn("Secondary Record Route Protection Subobject cType {} not supported", ctype);
                break;
        }
    }

    protected static ProtectionSubobject parseCommonProtectionBody(final short ctype, final ByteBuf byteBuf)
            throws RSVPParsingException {
        switch (ctype) {
            case PROTECTION_SUBOBJECT_TYPE_1:
                return parseCommonProtectionBodyType1(byteBuf);
            case PROTECTION_SUBOBJECT_TYPE_2:
                return parseCommonProtectionBodyType2(byteBuf);
            default:
                LOG.warn("Secondary Record Route Protection Subobject cType {} not supported", ctype);
                return null;
        }
    }
}
