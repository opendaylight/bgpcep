/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MemAsNumTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvLinkDescBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MemAsNumTlvParser.class);

    public static final int MEMBER_AS_NUMBER = 517;

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {
        final AsNumber memberAsn = new AsNumber(value.readUnsignedInt());
        LOG.debug("Parsed Member AsNumber {}", memberAsn);
        return memberAsn;
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        if (qNameId.equals(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID)) {
            final EpeNodeDescriptors epedescriptors = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
            if (epedescriptors.getMemberAsn() != null) {
                TlvUtil.writeTLV(MEMBER_AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(epedescriptors.getMemberAsn().getValue()).intValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID)) {
            final EpeNodeDescriptors eperemdescriptors = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
            if (eperemdescriptors.getMemberAsn() != null) {
                TlvUtil.writeTLV(MEMBER_AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(eperemdescriptors.getMemberAsn().getValue()).intValue()), buffer);
            }
        }
    }

    @Override
    public void buildLocalNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LocalNodeDescriptorsBuilder) tlvBuilder).setMemberAsn((AsNumber) subTlvObject);

    }

    @Override
    public void buildRemoteNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((RemoteNodeDescriptorsBuilder) tlvBuilder).setMemberAsn((AsNumber) subTlvObject);

    }
}
