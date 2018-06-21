/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yangtools.yang.common.QName;

public final class RemoteNodeDescriptorTlvParser extends AbstractNodeDescriptorTlvCodec implements LinkstateTlvParser<RemoteNodeDescriptors>, LinkstateTlvParser.LinkstateTlvSerializer<RemoteNodeDescriptors> {
    private static final int REMOTE_NODE_DESCRIPTORS_TYPE = 257;

    @Override
    public void serializeTlvBody(final RemoteNodeDescriptors tlv, final ByteBuf body) {
        serializeNodeDescriptor(tlv, body);
        final SimpleNlriTypeRegistry tlvReg = SimpleNlriTypeRegistry.getInstance();
        tlvReg.serializeTlv(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME, tlv.getBgpRouterId(), body);
        tlvReg.serializeTlv(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME, tlv.getMemberAsn(), body);
    }

    @Override
    public RemoteNodeDescriptors parseTlvBody(final ByteBuf value) {
        final Map<QName, Object> parsedSubTlvs = new HashMap<>();
        final RemoteNodeDescriptorsBuilder builder = new RemoteNodeDescriptorsBuilder(parseNodeDescriptor(value, parsedSubTlvs));
        builder.setBgpRouterId((Ipv4Address) parsedSubTlvs.get(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME));
        builder.setMemberAsn((AsNumber) parsedSubTlvs.get(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME));
        return builder.build();
    }

    @Override
    public QName getTlvQName() {
        return RemoteNodeDescriptors.QNAME;
    }

    @Override
    public int getType() {
        return REMOTE_NODE_DESCRIPTORS_TYPE;
    }

}
