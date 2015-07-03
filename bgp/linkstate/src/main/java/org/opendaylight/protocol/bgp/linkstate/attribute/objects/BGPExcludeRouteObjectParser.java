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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.ExcludeRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.Ipv6PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.Srlg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.SrlgBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.UnnumeredInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.UnnumeredInterfaceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BGPExcludeRouteObjectParser extends AbstractBGPObjectParser  {
    public static final short CLASS_NUM = 232;
    public static final short CTYPE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(BGPExcludeRouteObjectParser.class);
    private static final int SRLG_LENGTH = 8;
    private static final int AVOIDED = 128;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final ExcludeRouteObjectBuilder ero = new ExcludeRouteObjectBuilder();
        ero.setClassNum(CLASS_NUM);
        ero.setCType(CTYPE);
        List<SubobjectContainer> subObjectList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            int type = byteBuf.readUnsignedByte();
            boolean l = false;
            if (type > 127) {
                type = type - AVOIDED;
                l = true;
            }

            int length = byteBuf.readUnsignedByte();
            SubobjectContainerBuilder container = new SubobjectContainerBuilder();
            switch (type) {
            case BGPCommonExplicitRouteObjectParser.IPV4_PREFIX_TYPE:
                container.setSubobject(parseIpv4Prefix(byteBuf.readSlice(length)).setL(l).build());
                break;
            case BGPCommonExplicitRouteObjectParser.IPV6_PREFIX_TYPE:
                container.setSubobject(parseIpv6Prefix(byteBuf.readSlice(length)).setL(l).build());
                break;
            case BGPCommonExplicitRouteObjectParser.UNNUMERED_INTERFACE:
                container.setSubobject(parseUnnumeredInterface(byteBuf.readSlice(length)).setL(l).build());
                break;
            case BGPCommonExplicitRouteObjectParser.AS_NUMBER_TYPE:
                container.setSubobject(parseASNumber(byteBuf.readSlice(length)).build());
                break;
            case SLRG_TYPE:
                container.setSubobject(parseSRLG(byteBuf.readSlice(length)).setL(l).build());
                break;
            default:
                LOG.warn("Exclude Routes Subobject type {} not supported", type);
                break;
            }
            subObjectList.add(container.build());
        }
        return ero.setSubobjectContainer(subObjectList).build();
    }

    private SrlgBuilder parseSRLG(final ByteBuf byteBuf) {
        SrlgBuilder srlg = new SrlgBuilder();
        srlg.setSrlgId(byteBuf.readUnsignedInt());
        byteBuf.readShort();
        return srlg;
    }

    private AsNumberBuilder parseASNumber(final ByteBuf byteBuf) {
        AsNumberBuilder as = new AsNumberBuilder();
        as.setAsNumber(new AsNumber(byteBuf.readUnsignedInt()));
        return as;
    }

    private UnnumeredInterfaceBuilder parseUnnumeredInterface(final ByteBuf byteBuf) {
        UnnumeredInterfaceBuilder unnum = new UnnumeredInterfaceBuilder();
        byteBuf.readByte();
        unnum.setAttribute(byteBuf.readUnsignedByte());
        unnum.setTeRouterId(Ipv4Util.addressForByteBuf(byteBuf));
        unnum.setRouterId(byteBuf.readUnsignedInt());
        return unnum;
    }

    private Ipv6PrefixBuilder parseIpv6Prefix(final ByteBuf byteBuf) {
        Ipv6PrefixBuilder ipv6Case = new Ipv6PrefixBuilder();
        ipv6Case.setIpv6Address(Ipv6Util.addressForByteBuf(byteBuf));
        ipv6Case.setAttribute(byteBuf.readUnsignedByte());
        return ipv6Case;
    }

    private Ipv4PrefixBuilder parseIpv4Prefix(final ByteBuf byteBuf) {
        Ipv4PrefixBuilder ipv4Case = new Ipv4PrefixBuilder();
        ipv4Case.setIpv4Address(Ipv4Util.addressForByteBuf(byteBuf));
        ipv4Case.setAttribute(byteBuf.readUnsignedByte());
        return ipv4Case;
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof ExcludeRouteObject, "DetourObject is mandatory.");
        final ExcludeRouteObject excludeObject = (ExcludeRouteObject) teLspObject;

        int lenght = 0;
        final ByteBuf bufferAux = Unpooled.buffer();

        for (SubobjectContainer subObject : excludeObject.getSubobjectContainer()) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.exclude.route.
                object.exclude.route.object.subobject.container.Subobject sub = subObject.getSubobject();

            if (sub instanceof Ipv4Prefix) {
                lenght = +IPV4_PREFIX_LENGTH;
                final Ipv4Prefix ipv4Prefix = (Ipv4Prefix) sub;
                if (ipv4Prefix.isL()) {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.IPV4_PREFIX_TYPE + AVOIDED);
                } else {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.IPV4_PREFIX_TYPE);
                }
                bufferAux.writeByte(IPV4_PREFIX_LENGTH);
                bufferAux.writeBytes(Ipv4Util.byteBufForAddress(ipv4Prefix.getIpv4Address()));
                bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.IPV4_BITS_LENGTH);
                bufferAux.writeByte(ipv4Prefix.getAttribute());
            } else if (sub instanceof Ipv6Prefix) {
                lenght = +IPV6_PREFIX_LENGTH;
                final Ipv6Prefix ipv6Prefix = (Ipv6Prefix) sub;
                if (ipv6Prefix.isL()) {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.IPV6_PREFIX_TYPE + AVOIDED);
                } else {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.IPV6_PREFIX_TYPE);
                }
                bufferAux.writeByte(IPV6_PREFIX_LENGTH);
                bufferAux.writeBytes(Ipv6Util.byteBufForAddress(ipv6Prefix.getIpv6Address()));
                bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.IPV6_BITS_LENGTH);
                bufferAux.writeByte(ipv6Prefix.getAttribute());
            } else if (sub instanceof UnnumeredInterface) {
                lenght = +BGPCommonExplicitRouteObjectParser.UNNUMERED_LENGTH;
                final UnnumeredInterface unnum = (UnnumeredInterface) sub;
                if (unnum.isL()) {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.UNNUMERED_INTERFACE + AVOIDED);
                } else {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.UNNUMERED_INTERFACE);
                }
                bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.UNNUMERED_LENGTH);
                bufferAux.writeZero(RESERVED);
                bufferAux.writeByte(unnum.getAttribute());
                bufferAux.writeBytes(Ipv4Util.byteBufForAddress(unnum.getTeRouterId()));
                bufferAux.writeLong(unnum.getRouterId());
            } else if (sub instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp
                .rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.AsNumber) {
                lenght = +AUTONOMUS_SYSTEM_LENGTH;
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp
                    .rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.AsNumber as =
                    (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp
                        .rev150706.exclude.route.object.exclude.route.object.subobject.container.subobject.AsNumber) sub;
                if (as.isL()) {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.AS_NUMBER_TYPE + AVOIDED);
                } else {
                    bufferAux.writeByte(BGPCommonExplicitRouteObjectParser.AS_NUMBER_TYPE);
                }
                bufferAux.writeByte(AUTONOMUS_SYSTEM_LENGTH);
            } else if (sub instanceof Srlg) {
                final Srlg srlg = (Srlg) sub;
                lenght = +SRLG_LENGTH;

                if (srlg.isL()) {
                    bufferAux.writeByte(SLRG_TYPE + AVOIDED);
                } else {
                    bufferAux.writeByte(SLRG_TYPE);
                }

                bufferAux.writeLong(srlg.getSrlgId());
                bufferAux.writeZero(RESERVED);
                bufferAux.writeZero(RESERVED);
            }
        }
        serializeAttributeHeader(lenght, excludeObject.getClassNum(), excludeObject.getCType(), output);
        output.writeBytes(bufferAux);
    }
}
