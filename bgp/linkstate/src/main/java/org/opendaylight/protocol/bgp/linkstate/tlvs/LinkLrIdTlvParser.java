/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinkLrIdTlvParser implements NlriSubTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(LinkLrIdTlvParser.class);

    public static final int LINK_LR_IDENTIFIERS = 258;

    static void parseLinkLrIdSubTlvObject(final ByteBuf value, final LinkDescriptorsBuilder linkBuilder) {
        linkBuilder.setLinkLocalIdentifier(value.readUnsignedInt());
        linkBuilder.setLinkRemoteIdentifier(value.readUnsignedInt());
        LOG.debug("Parsed link local {} remote {} Identifiers.",linkBuilder.getLinkLocalIdentifier(), linkBuilder.getLinkRemoteIdentifier());
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        final LinkDescriptors ldescriptor = ((LinkCase)nlriTypeCase).getLinkDescriptors();
        if (ldescriptor.getLinkLocalIdentifier() != null && ldescriptor.getLinkRemoteIdentifier() != null) {
            final ByteBuf identifierBuf = Unpooled.buffer();
            identifierBuf.writeInt(ldescriptor.getLinkLocalIdentifier().intValue());
            identifierBuf.writeInt(ldescriptor.getLinkRemoteIdentifier().intValue());
            TlvUtil.writeTLV(LINK_LR_IDENTIFIERS, identifierBuf, buffer);
        }
    }
}
