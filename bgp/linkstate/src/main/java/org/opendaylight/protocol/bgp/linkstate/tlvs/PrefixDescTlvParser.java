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
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrefixDescTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixDescTlvParser.class);

    @Override
    public Object parseNlriTlvObject(final ByteBuf buffer, final NlriType nlriType) throws BGPParsingException {

        final PrefixDescriptorsBuilder prefixBuilder = new PrefixDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsing {} Descriptor: {}", nlriType, ByteBufUtil.hexDump(value));
            }
            final Object subTlvObject = SimpleNlriTypeRegistry.getInstance().parseSubTlvObject(value, type, nlriType);
            final SubTlvPrefixBuilder prefixBuilderObject = (SubTlvPrefixBuilder) SimpleNlriTypeRegistry.getInstance().getSubTlvParser(type);
            prefixBuilderObject.buildPrefixDescriptor(subTlvObject, prefixBuilder);
        }
        return prefixBuilder.build();
    }

    @Override
    public void serializeNlriTlvObject(final ObjectType tlvObject, final NodeIdentifier qNameId, final NlriType nlriType, ByteBuf localdescs) {
        final SimpleNlriTypeRegistry nlriReg = SimpleNlriTypeRegistry.getInstance();
        nlriReg.serializeSubTlvObject(tlvObject, TlvUtil.MULTI_TOPOLOGY_ID, qNameId, localdescs);
        nlriReg.serializeSubTlvObject(tlvObject, OspfRTypeTlvParser.OSPF_ROUTE_TYPE, qNameId, localdescs);
        nlriReg.serializeSubTlvObject(tlvObject, IpReachTlvParser.IP_REACHABILITY, qNameId, localdescs);
    }
}
