/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinkLrIdTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(LinkLrIdTlvParser.class);

    private static final int LINK_LR_IDENTIFIERS = 258;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        final LinkDescriptorsBuilder linkbuilder = context.getLinkDescriptorsBuilder();
        linkbuilder.setLinkLocalIdentifier(value.readUnsignedInt());
        linkbuilder.setLinkRemoteIdentifier(value.readUnsignedInt());
        LOG.debug("Parsed link local {} remote {} Identifiers.",linkbuilder.getLinkLocalIdentifier(), linkbuilder.getLinkRemoteIdentifier());
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        final LinkDescriptors ldescriptor = ((LinkCase)nlriTypeCase).getLinkDescriptors();
        if (ldescriptor.getLinkLocalIdentifier() != null && ldescriptor.getLinkRemoteIdentifier() != null) {
            final ByteBuf identifierBuf = Unpooled.buffer();
            identifierBuf.writeInt(ldescriptor.getLinkLocalIdentifier().intValue());
            identifierBuf.writeInt(ldescriptor.getLinkRemoteIdentifier().intValue());
            TlvUtil.writeTLV(LINK_LR_IDENTIFIERS, identifierBuf, buffer);
        }
    }
}
