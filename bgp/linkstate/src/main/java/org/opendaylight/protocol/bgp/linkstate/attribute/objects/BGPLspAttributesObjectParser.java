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
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.attribute.flags.FlagContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.attribute.flags.FlagContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.att.subobject.LspSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.att.subobject.lsp.subobject.FlagsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.att.subobject.lsp.subobject.FlagsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.attributes.object.LspAttributesObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.attributes.object.LspAttributesObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.attributes.object.lsp.attributes.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.lsp.attributes.object.lsp.attributes.object.SubobjectContainerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPLspAttributesObjectParser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 197;
    public static final short CTYPE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(BGPLspAttributesObjectParser.class);
    protected static final int FLAG_TLV_TYPE = 1;
    protected static final int FLAG_TLV_SIZE = 4;
    protected static final int TLV_HEADER_SIZE = 4;

    protected static List<FlagContainer> parseFlag(final ByteBuf byteBuf) {
        List<FlagContainer> flagList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final BitArray flags0_7 = BitArray.valueOf(byteBuf.readByte());
            final BitArray flags8_15 = BitArray.valueOf(byteBuf.readByte());
            final BitArray flags16_23 = BitArray.valueOf(byteBuf.readByte());
            final BitArray flags24_32 = BitArray.valueOf(byteBuf.readByte());
            // FlagsTlvBuilder flagBuilder = new FlagsTlvBuilder();
            FlagContainerBuilder flagBuilder = new FlagContainerBuilder();
            flagBuilder.setFlags(new FlagContainer.Flags(flags0_7.get(0), flags8_15.get(1), flags8_15.get(2),
                flags8_15.get(3), flags8_15.get(4), flags8_15.get(5), flags8_15.get(6),
                flags8_15.get(7), flags16_23.get(0), flags16_23.get(1), flags16_23.get(2),
                flags16_23.get(3), flags0_7.get(1), flags16_23.get(4), flags16_23.get(5),
                flags16_23.get(6), flags16_23.get(7), flags24_32.get(0), flags24_32.get(1),
                flags24_32.get(2), flags24_32.get(3), flags24_32.get(4), flags24_32.get(5),
                flags0_7.get(2), flags24_32.get(6), flags24_32.get(7), flags0_7.get(3),
                flags0_7.get(4), flags0_7.get(5), flags0_7.get(6), flags0_7.get(7),
                flags8_15.get(0)));
            flagList.add(flagBuilder.build());
        }
        return flagList;
    }

    protected static void serializeFlag(final List<FlagContainer> flagList, final ByteBuf bufferAux) {
        for (FlagContainer flagContainer : flagList) {
            final FlagContainer.Flags flag = flagContainer.getFlags();
            serializeBitArray(bufferAux, flag.isPosition1(), flag.isPosition2(), flag.isPosition3(), flag.isPosition4(),
                flag.isPosition5(), flag.isPosition6(), flag.isPosition7(), flag.isPosition8());
            serializeBitArray(bufferAux, flag.isPosition9(), flag.isPosition10(), flag.isPosition11(), flag.isPosition12(),
                flag.isPosition13(), flag.isPosition14(), flag.isPosition15(), flag.isPosition16());
            serializeBitArray(bufferAux, flag.isPosition17(), flag.isPosition18(), flag.isPosition19(), flag.isPosition20(),
                flag.isPosition21(), flag.isPosition22(), flag.isPosition23(), flag.isPosition24());
            serializeBitArray(bufferAux, flag.isPosition25(), flag.isPosition26(), flag.isPosition27(), flag.isPosition28(),
                flag.isPosition29(), flag.isPosition30(), flag.isPosition31(), flag.isPosition32());
        }
    }

    private static void serializeBitArray(final ByteBuf bufferAux, final Boolean... position) {
        final BitArray bitArray = new BitArray(FLAGS_SIZE);
        for (int i = 0; i < 8; i++) {
            bitArray.set(i, position[i]);
        }
        bitArray.toByteBuf(bufferAux);
    }

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        LspAttributesObjectBuilder builder = new LspAttributesObjectBuilder();
        builder.setCType(CTYPE);
        builder.setClassNum(CLASS_NUM);

        List<SubobjectContainer> subObjectList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final int type = byteBuf.readUnsignedShort();
            final int length = byteBuf.readUnsignedShort();
            final ByteBuf value = byteBuf.readSlice(length);
            SubobjectContainerBuilder subObj = new SubobjectContainerBuilder();
            switch (type) {
            case FLAG_TLV_TYPE:
                subObj.setLspSubobject(new FlagsTlvBuilder().setFlagContainer(parseFlag(value)).build());
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
        Preconditions.checkArgument(teLspObject instanceof LspAttributesObject, "LspAttributesObject is mandatory.");
        final LspAttributesObject lspAttributesObject = (LspAttributesObject) teLspObject;

        final ByteBuf bufferAux = Unpooled.buffer();
        int lenght = 0;
        for (SubobjectContainer subObject : lspAttributesObject.getSubobjectContainer()) {
            final LspSubobject lspSubonject = subObject.getLspSubobject();
            if (lspSubonject instanceof FlagsTlv) {
                final ByteBuf flagTLVValue = Unpooled.buffer();
                final List<FlagContainer> flagList = ((FlagsTlv) lspSubonject).getFlagContainer();
                lenght = FLAG_TLV_SIZE * flagList.size();
                serializeFlag(flagList, flagTLVValue);
                serializeTLV(FLAG_TLV_TYPE, lenght, flagTLVValue, bufferAux);
                lenght += TLV_HEADER_SIZE;
            }
        }

        serializeAttributeHeader(lenght, lspAttributesObject.getClassNum(), lspAttributesObject.getCType(), byteAggregator);
        byteAggregator.writeBytes(bufferAux);
    }

    protected static void serializeTLV(final int tlvType, final int lenght, final ByteBuf value, final ByteBuf auxBuffer) {
        auxBuffer.writeShort(tlvType);
        auxBuffer.writeShort(lenght);
        auxBuffer.writeBytes(value);
    }
}
