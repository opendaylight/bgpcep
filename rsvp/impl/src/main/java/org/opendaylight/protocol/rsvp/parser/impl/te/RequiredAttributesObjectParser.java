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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.attribute.flags.FlagContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.att.subobject.LspSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.att.subobject.lsp.subobject.FlagsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.att.subobject.lsp.subobject.FlagsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.LspAttributesObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.lsp.attributes.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.attributes.object.lsp.attributes.object.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.required.attributes.object.LspRequiredAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.lsp.required.attributes.object.LspRequiredAttributesObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequiredAttributesObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 67;
    public static final short CTYPE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(RequiredAttributesObjectParser.class);

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final LspRequiredAttributesObjectBuilder builder = new LspRequiredAttributesObjectBuilder();

        final List<SubobjectContainer> subObjectList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final int type = byteBuf.readUnsignedShort();
            final int length = byteBuf.readUnsignedShort();
            final ByteBuf value = byteBuf.readSlice(length);
            final SubobjectContainerBuilder subObj = new SubobjectContainerBuilder();
            if (type == AttributesObjectParser.FLAG_TLV_TYPE) {
                subObj.setLspSubobject(new FlagsTlvBuilder().setFlagContainer(AttributesObjectParser.parseFlag(value)).build());
            } else {
                LOG.warn("Lsp Attributes Subobject type {} not supported", type);
            }
            subObjectList.add(subObj.build());
        }

        return builder.setLspAttributesObject(new LspAttributesObjectBuilder().setSubobjectContainer(subObjectList).build()).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof LspRequiredAttributesObject, "LspAttributesObject is mandatory.");
        final LspRequiredAttributesObject lspAttributesObject = (LspRequiredAttributesObject) teLspObject;

        final ByteBuf bufferAux = Unpooled.buffer();
        int lenght = 0;
        for (SubobjectContainer subObject : lspAttributesObject.getLspAttributesObject().getSubobjectContainer()) {
            final LspSubobject lspSubonject = subObject.getLspSubobject();
            if (lspSubonject instanceof FlagsTlv) {
                final ByteBuf flagTLVValue = Unpooled.buffer();
                final List<FlagContainer> flagList = ((FlagsTlv) lspSubonject).getFlagContainer();
                lenght = AttributesObjectParser.FLAG_TLV_SIZE * flagList.size();
                AttributesObjectParser.serializeFlag(flagList, flagTLVValue);
                AttributesObjectParser.serializeTLV(AttributesObjectParser.FLAG_TLV_TYPE, lenght, flagTLVValue, bufferAux);
                lenght += AttributesObjectParser.TLV_HEADER_SIZE;
            }
        }
        serializeAttributeHeader(lenght, CLASS_NUM, CTYPE, byteAggregator);
        byteAggregator.writeBytes(bufferAux);
    }
}
