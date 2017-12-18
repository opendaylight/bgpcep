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
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.attribute.flags.FlagContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.attribute.flags.FlagContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.att.subobject.LspSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.att.subobject.lsp.subobject.FlagsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.att.subobject.lsp.subobject.FlagsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.LspAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.LspAttributesObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.lsp.attributes.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.lsp.attributes.object.SubobjectContainerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AttributesObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 197;
    public static final short CTYPE = 1;
    static final int FLAG_TLV_TYPE = 1;
    static final int FLAG_TLV_SIZE = 4;
    static final int TLV_HEADER_SIZE = 4;
    private static final Logger LOG = LoggerFactory.getLogger(AttributesObjectParser.class);

    static List<FlagContainer> parseFlag(final ByteBuf byteBuf) {
        final List<FlagContainer> flagList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final byte[] value = ByteArray.readBytes(byteBuf, FLAG_TLV_SIZE);
            final FlagContainerBuilder flagBuilder = new FlagContainerBuilder().setFlags(value);
            flagList.add(flagBuilder.build());
        }
        return flagList;
    }

    static void serializeFlag(final List<FlagContainer> flagList, final ByteBuf bufferAux) {
        for (final FlagContainer flagContainer : flagList) {
            bufferAux.writeBytes(flagContainer.getFlags());
        }
    }

    static void serializeTLV(final int tlvType, final int lenght, final ByteBuf value, final ByteBuf
        auxBuffer) {
        auxBuffer.writeShort(tlvType);
        auxBuffer.writeShort(lenght);
        auxBuffer.writeBytes(value);
    }

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final LspAttributesObjectBuilder builder = new LspAttributesObjectBuilder();

        final List<SubobjectContainer> subObjectList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final int type = byteBuf.readUnsignedShort();
            final int length = byteBuf.readUnsignedShort();
            final ByteBuf value = byteBuf.readSlice(length);
            final SubobjectContainerBuilder subObj = new SubobjectContainerBuilder();
            if (type == FLAG_TLV_TYPE) {
                subObj.setLspSubobject(new FlagsTlvBuilder().setFlagContainer(parseFlag(value)).build());
            } else {
                LOG.warn("Lsp Attributes Subobject type {} not supported", type);
            }
            subObjectList.add(subObj.build());
        }

        return builder.setSubobjectContainer(subObjectList).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof LspAttributesObject, "LspAttributesObject is mandatory.");
        final LspAttributesObject lspAttributesObject = (LspAttributesObject) teLspObject;

        final ByteBuf bufferAux = Unpooled.buffer();
        int length = 0;
        for (final SubobjectContainer subObject : lspAttributesObject.getSubobjectContainer()) {
            final LspSubobject lspSubonject = subObject.getLspSubobject();
            if (lspSubonject instanceof FlagsTlv) {
                final ByteBuf flagTLVValue = Unpooled.buffer();
                final List<FlagContainer> flagList = ((FlagsTlv) lspSubonject).getFlagContainer();
                length = FLAG_TLV_SIZE * flagList.size();
                serializeFlag(flagList, flagTLVValue);
                serializeTLV(FLAG_TLV_TYPE, length, flagTLVValue, bufferAux);
                length += TLV_HEADER_SIZE;
            }
        }

        serializeAttributeHeader(length, CLASS_NUM, CTYPE, byteAggregator);
        byteAggregator.writeBytes(bufferAux);
    }
}
