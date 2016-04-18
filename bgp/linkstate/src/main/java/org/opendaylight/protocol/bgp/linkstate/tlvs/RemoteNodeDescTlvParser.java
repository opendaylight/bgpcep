/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RemoteNodeDescTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteNodeDescTlvParser.class);

    @Override
    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier parseNlriTlvObject(final ByteBuf buffer, final NlriType nlriType) throws BGPParsingException {
        final RemoteNodeDescriptorsBuilder remNodeBuilder = new RemoteNodeDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsing {} Descriptor: {}", nlriType, ByteBufUtil.hexDump(value));
            }
            final Object subTlvObject = SimpleNlriTypeRegistry.getInstance().parseSubTlvObject(value, type, nlriType);
            final SubTlvLinkDescBuilder localBuilderObject = (SubTlvLinkDescBuilder) SimpleNlriTypeRegistry.getInstance().getSubTlvParser(type);
            localBuilderObject.buildRemoteNodeDescriptor(subTlvObject, remNodeBuilder);
        }
        return remNodeBuilder.build();
    }

    @Override
    public void serializeNlriTlvObject(final ObjectType tlvObject, final NodeIdentifier qNameId, final NlriType nlriType, final ByteBuf localdescs) {
        final SimpleNlriTypeRegistry nlriReg = SimpleNlriTypeRegistry.getInstance();
        nlriReg.serializeSubTlvObject(tlvObject, AsNumTlvParser.AS_NUMBER, qNameId, localdescs);
        nlriReg.serializeSubTlvObject(tlvObject, DomainIdTlvParser.BGP_LS_ID, qNameId, localdescs);
        nlriReg.serializeSubTlvObject(tlvObject, AreaIdTlvParser.AREA_ID, qNameId, localdescs);
        nlriReg.serializeSubTlvObject(tlvObject, CrouterIdTlvParser.IGP_ROUTER_ID, qNameId, localdescs);
        nlriReg.serializeSubTlvObject(tlvObject, BgpRouterIdTlvParser.BGP_ROUTER_ID, qNameId, localdescs);
        nlriReg.serializeSubTlvObject(tlvObject, MemAsNumTlvParser.MEMBER_AS_NUMBER, qNameId, localdescs);
    }
}
