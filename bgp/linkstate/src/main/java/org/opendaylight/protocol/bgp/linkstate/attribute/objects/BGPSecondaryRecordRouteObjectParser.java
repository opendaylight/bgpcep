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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.protection.object.CommonProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.CommonRecordRouteSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.CommonRecordRouteSubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.SubojectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.SubojectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.common.record.route.subobject.common.record.route.subobject.suboject.container.subobject.ProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.secondary.record.route.object.SecondaryRecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.secondary.record.route.object.SecondaryRecordRouteObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPSecondaryRecordRouteObjectParser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 201;
    public static final short CTYPE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(BGPSecondaryRecordRouteObjectParser.class);
    private static final int HEADER_LENGHT = 4;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final SecondaryRecordRouteObjectBuilder srro = new SecondaryRecordRouteObjectBuilder();
        srro.setClassNum(CLASS_NUM);
        srro.setCType(CTYPE);

        List<SubojectContainer> subObjectList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final int type = byteBuf.readUnsignedByte();
            final int length = byteBuf.readUnsignedByte();
            SubojectContainerBuilder subBuilder = new SubojectContainerBuilder();
            switch (type) {
            case BGPCommonRecordRouteObjectParser.IPV4_PREFIX_TYPE:
                subBuilder.setSubobject(BGPCommonRecordRouteObjectParser.parseIPV4Prefix(byteBuf.readSlice(length)));
                break;
            case BGPCommonRecordRouteObjectParser.IPV6_PREFIX_TYPE:
                subBuilder.setSubobject(BGPCommonRecordRouteObjectParser.parseIPV6Prefix(byteBuf.readSlice(length)));
                break;
            case BGPCommonRecordRouteObjectParser.LABEL_TYPE:
                subBuilder.setSubobject(BGPCommonRecordRouteObjectParser.parseLabel(byteBuf.readSlice(length)));
                break;
            case PROTECTION_TYPE:
                subBuilder.setSubobject(BGPCommonRecordRouteObjectParser.parseProtectionSubObject(byteBuf.readSlice(length)));
                break;
            default:
                LOG.warn("Record Route Subobject type {} not supported", type);
                break;
            }
            subObjectList.add(subBuilder.build());
        }
        srro.setCommonRecordRouteSubobject(new CommonRecordRouteSubobjectBuilder().setSubojectContainer(subObjectList).build());
        return srro.build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof SecondaryRecordRouteObject, "RecordRouteObject is mandatory.");
        final SecondaryRecordRouteObject recordObject = (SecondaryRecordRouteObject) teLspObject;

        int lenght;
        final ByteBuf bufferAux = Unpooled.buffer();
        final CommonRecordRouteSubobject common = recordObject.getCommonRecordRouteSubobject();
        lenght =+ HEADER_LENGHT;
        for (SubojectContainer subObject : common.getSubojectContainer()) {
            final Subobject sub = subObject.getSubobject();
            if (sub instanceof Ipv4Case) {
                final Ipv4Case ipv4Case = (Ipv4Case) sub;
                BGPCommonRecordRouteObjectParser.serializeIPV4Prefix(ipv4Case, bufferAux);
                lenght = +BGPCommonRecordRouteObjectParser.IPV4_CASE_LENGTH;
            } else if (sub instanceof Ipv6Case) {
                final Ipv6Case ipv6Case = (Ipv6Case) sub;
                BGPCommonRecordRouteObjectParser.serializeIPV6Prefix(ipv6Case, bufferAux);
                lenght = +BGPCommonRecordRouteObjectParser.IPV6_CASE_LENGTH;
            } else if (sub instanceof LabelCase) {
                final LabelCase labelCase = (LabelCase) sub;
                BGPCommonRecordRouteObjectParser.serializeLabel(labelCase, bufferAux);
                lenght = +BGPCommonRecordRouteObjectParser.LABEL_CASE_LENGTH;
            } else if (sub instanceof ProtectionCase) {
                final CommonProtectionObject protectionCase = ((ProtectionCase) sub).getCommonProtectionObject();
                switch (protectionCase.getCType())
                {
                case BGPCommonExplicitRouteObjectParser.PROTECTION_SUBOBJECT_TYPE_1:
                    lenght = +BGPCommonExplicitRouteObjectParser.PROTECTION_SUBOBJECT_TYPE1_BODY_LENGTH;
                    BGPCommonExplicitRouteObjectParser.serializeBodyType1(protectionCase,bufferAux);
                    break;
                case BGPCommonExplicitRouteObjectParser.PROTECTION_SUBOBJECT_TYPE_2:
                    lenght = +BGPCommonExplicitRouteObjectParser.PROTECTION_SUBOBJECT_TYPE2_BODY_LENGTH;
                    BGPCommonExplicitRouteObjectParser.serializeBodyType2(protectionCase,bufferAux);
                    break;
                }
            }
        }

        serializeAttributeHeader(lenght, recordObject.getClassNum(), recordObject.getCType(), output);
        output.writeBytes(bufferAux);
    }
}
