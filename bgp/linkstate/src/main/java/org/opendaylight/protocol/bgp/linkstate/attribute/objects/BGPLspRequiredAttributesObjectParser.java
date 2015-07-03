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

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.attribute.flags.FlagContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.att.subobject.LspSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.att.subobject.lsp.subobject.FlagsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.att.subobject.lsp.subobject.FlagsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.required.attributes.object.LspRequiredAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.required.attributes.object.LspRequiredAttributesObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.required.attributes.object.lsp.required.attributes.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.required.attributes.object.lsp.required.attributes.object.SubobjectContainerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPLspRequiredAttributesObjectParser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 67;
    public static final short CTYPE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(BGPLspRequiredAttributesObjectParser.class);

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        LspRequiredAttributesObjectBuilder builder = new LspRequiredAttributesObjectBuilder();
        builder.setCType(CTYPE);
        builder.setClassNum(CLASS_NUM);

        List<SubobjectContainer> subObjectList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final int type = byteBuf.readUnsignedShort();
            final int length = byteBuf.readUnsignedShort();
            final ByteBuf value = byteBuf.readSlice(length);
            SubobjectContainerBuilder subObj = new SubobjectContainerBuilder();
            switch (type) {
            case BGPLspAttributesObjectParser.FLAG_TLV_TYPE:
                subObj.setLspSubobject(new FlagsTlvBuilder().setFlagContainer(BGPLspAttributesObjectParser.parseFlag(value)).build());
                break;
            default:
                LOG.warn("Lsp Attributes Subobject type {} not supported", type);
                break;
            }
            subObjectList.add(subObj.build());
        }

        return builder.setSubobjectContainer(subObjectList).build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof LspRequiredAttributesObject, "LspAttributesObject is mandatory.");
        final LspRequiredAttributesObject lspAttributesObject = (LspRequiredAttributesObject) teLspObject;

        final ByteBuf bufferAux = Unpooled.buffer();
        int lenght = 0;
        for (SubobjectContainer subObject : lspAttributesObject.getSubobjectContainer()) {
            final LspSubobject lspSubonject = subObject.getLspSubobject();
            if (lspSubonject instanceof FlagsTlv) {
                final ByteBuf flagTLVValue = Unpooled.buffer();
                final List<FlagContainer> flagList = ((FlagsTlv) lspSubonject).getFlagContainer();
                lenght = BGPLspAttributesObjectParser.FLAG_TLV_SIZE * flagList.size();
                BGPLspAttributesObjectParser.serializeFlag(flagList, flagTLVValue);
                BGPLspAttributesObjectParser.serializeTLV(BGPLspAttributesObjectParser.FLAG_TLV_TYPE, lenght, flagTLVValue, bufferAux);
                lenght += BGPLspAttributesObjectParser.TLV_HEADER_SIZE;
            }
        }
        serializeAttributeHeader(lenght, lspAttributesObject.getClassNum(), lspAttributesObject.getCType(), byteAggregator);
        byteAggregator.writeBytes(bufferAux);
    }
}
