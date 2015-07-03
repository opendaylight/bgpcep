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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.primary.path.route.object.PrimaryPathRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.primary.path.route.object.PrimaryPathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.Ipv4PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.Ipv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.subobject.explicit.route.subobject.container.subobject.UnnumeredInterfaceCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPPrimaryPathRouteObject extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 38;
    public static final short CTYPE = 1;

    private static final Logger LOG = LoggerFactory.getLogger(BGPPrimaryPathRouteObject.class);

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final PrimaryPathRouteObjectBuilder primPath = new PrimaryPathRouteObjectBuilder();
        primPath.setClassNum(CLASS_NUM);
        primPath.setCType(CTYPE);

        List<SubobjectContainer> subObjectList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            int type = byteBuf.readUnsignedByte();
            boolean l = false;
            if (type > 127) {
                type = type - BGPCommonExplicitRouteObjectParser.LOOSE;
                l = true;
            }

            int length = byteBuf.readUnsignedByte();
            SubobjectContainerBuilder container = new SubobjectContainerBuilder();
            switch (type) {
            case BGPCommonExplicitRouteObjectParser.IPV4_PREFIX_TYPE:
                container.setSubobject(BGPCommonExplicitRouteObjectParser.parseIpv4Prefix(byteBuf.readSlice(length), l));
                break;
            case BGPCommonExplicitRouteObjectParser.IPV6_PREFIX_TYPE:
                container.setSubobject(BGPCommonExplicitRouteObjectParser.parseIpv6Prefix(byteBuf.readSlice(length), l));
                break;
            case BGPCommonExplicitRouteObjectParser.UNNUMERED_INTERFACE:
                container.setSubobject(BGPCommonExplicitRouteObjectParser.parseUnnumeredInterface(byteBuf.readSlice(length), l));
                break;
            default:
                LOG.warn("Exclude Routes Subobject type {} not supported", type);
                break;
            }
            subObjectList.add(container.build());
        }
        return primPath.setSubobjectContainer(subObjectList).build();
    }


    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof PrimaryPathRouteObject, "PrimaryPathRouteObject is mandatory.");
        final PrimaryPathRouteObject primObject = (PrimaryPathRouteObject) teLspObject;
        int lenght = 0;
        final ByteBuf bufferAux = Unpooled.buffer();
        for (SubobjectContainer subObject : primObject.getSubobjectContainer()) {
            final Subobject sub = subObject.getSubobject();
            if (sub instanceof Ipv4PrefixCase) {
                lenght = +IPV4_PREFIX_LENGTH;
                final Ipv4PrefixCase ipv4Prefix = (Ipv4PrefixCase) sub;
                BGPCommonExplicitRouteObjectParser.serializeIPV4Prefix(ipv4Prefix, bufferAux);
            } else if (sub instanceof Ipv6PrefixCase) {
                lenght = +IPV6_PREFIX_LENGTH;
                final Ipv6PrefixCase ipv6Prefix = (Ipv6PrefixCase) sub;
                BGPCommonExplicitRouteObjectParser.serializeIPV6Prefix(ipv6Prefix, bufferAux);
            } else if (sub instanceof UnnumeredInterfaceCase) {
                lenght = +BGPCommonExplicitRouteObjectParser.UNNUMERED_LENGTH;
                final UnnumeredInterfaceCase unnum = (UnnumeredInterfaceCase) sub;
                BGPCommonExplicitRouteObjectParser.serializeUnnumbered(unnum, bufferAux);
            }
        }
        serializeAttributeHeader(lenght, primObject.getClassNum(), primObject.getCType(), output);
        output.writeBytes(bufferAux);
    }
}
