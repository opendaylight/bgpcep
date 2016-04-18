/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MemAsNumTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(MemAsNumTlvParser.class);

    private static final int MEMBER_AS_NUMBER = 517;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        final LinkNlriParser linkparser = new LinkNlriParser();
        final AsNumber memberAsn = new AsNumber(value.readUnsignedInt());
        LOG.debug("Parsed Member AsNumber {}", memberAsn);
        linkparser.setMemAsNumBuilder(memberAsn, context);
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        if (SimpleNlriTypeRegistry.getInstance().isLocal()) {
            final EpeNodeDescriptors epedescriptors = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
            if (epedescriptors.getMemberAsn() != null) {
                TlvUtil.writeTLV(MEMBER_AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(epedescriptors.getMemberAsn().getValue()).intValue()), buffer);
            }
        } else {
            final EpeNodeDescriptors eperemdescriptors = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
            if (eperemdescriptors.getMemberAsn() != null) {
                TlvUtil.writeTLV(MEMBER_AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(eperemdescriptors.getMemberAsn().getValue()).intValue()), buffer);
            }
        }
    }
}
