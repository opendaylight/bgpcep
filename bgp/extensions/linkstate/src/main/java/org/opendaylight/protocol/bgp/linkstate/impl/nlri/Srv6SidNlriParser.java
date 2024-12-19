/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.SRv6EndpointTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.SRv6PeerNodeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.SRv6SidInformationTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.SRv6SidStructureTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractNlriTypeCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.Srv6SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.Srv6SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6SidInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6SidInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.attributes.Srv6BgpPeerNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.attributes.Srv6EndpointBehavior;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.sid.subtlvs.Srv6SidStructure;
import org.opendaylight.yangtools.yang.common.QName;

public final class Srv6SidNlriParser extends AbstractNlriTypeCodec {

    @Override
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final NodeIdentifier srv6Node = SimpleNlriTypeRegistry.getInstance().parseTlv(buffer);
        final Srv6SidInformation srv6SidInformation = SimpleNlriTypeRegistry.getInstance().parseTlv(buffer);
        return new Srv6SidCaseBuilder()
                .setSrv6NodeDescriptors(new Srv6NodeDescriptorsBuilder(srv6Node).build())
                .setSrv6SidInformation(new Srv6SidInformationBuilder(srv6SidInformation).build())
                .setSrv6Attributes(parseSrv6Attributes(buffer))
                .build();
    }

    @Override
    protected void serializeObjectType(final ObjectType objectType, final ByteBuf buffer) {
        final Srv6SidCase srv6 = (Srv6SidCase) objectType;
        SimpleNlriTypeRegistry.getInstance().serializeTlv(Srv6NodeDescriptors.QNAME,
            srv6.getSrv6NodeDescriptors(), buffer);
        SimpleNlriTypeRegistry.getInstance().serializeTlv(SRv6SidInformationTlvParser.SRV6_SID_INFORMATION_QNAME,
            srv6.getSrv6SidInformation(), buffer);
        serializeSrv6Attributes(srv6.getSrv6Attributes(), buffer);
    }

    private static Srv6Attributes parseSrv6Attributes(final ByteBuf buffer) {
        final Map<QName, Object> tlvs = SimpleNlriTypeRegistry.getInstance().parseSubTlvs(buffer);
        final Srv6AttributesBuilder builder = new Srv6AttributesBuilder();
        builder.setSrv6EndpointBehavior(
            (Srv6EndpointBehavior) tlvs.get(SRv6EndpointTlvParser.SRV6_ENDPOINT_BEHAVIOR_QNAME));
        builder.setSrv6BgpPeerNode((Srv6BgpPeerNode) tlvs.get(SRv6PeerNodeTlvParser.SRV6_BGP_PEER_NODE_QNAME));
        builder.setSrv6SidStructure((Srv6SidStructure) tlvs.get(SRv6SidStructureTlvParser.SRV6_SID_STRUCTURE_QNAME));
        return builder.build();
    }

    private static void serializeSrv6Attributes(final Srv6Attributes tlv, final ByteBuf buffer) {
        final SimpleNlriTypeRegistry reg = SimpleNlriTypeRegistry.getInstance();
        reg.serializeTlv(SRv6EndpointTlvParser.SRV6_ENDPOINT_BEHAVIOR_QNAME, tlv.getSrv6EndpointBehavior(), buffer);
        reg.serializeTlv(SRv6PeerNodeTlvParser.SRV6_BGP_PEER_NODE_QNAME, tlv.getSrv6BgpPeerNode(), buffer);
        reg.serializeTlv(SRv6SidStructureTlvParser.SRV6_SID_STRUCTURE_QNAME, tlv.getSrv6SidStructure(), buffer);
    }

    @Override
    public int getNlriType() {
        return NlriType.Srv6Sid.getIntValue();
    }
}
