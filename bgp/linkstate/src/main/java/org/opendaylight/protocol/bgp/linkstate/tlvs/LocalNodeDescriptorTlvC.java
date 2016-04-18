/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;

public class LocalNodeDescriptorTlvC implements LinkstateEpeNodeTlvParser<NodeIdentifier>, LinkstateTlvSerializer<NodeIdentifier> {

    public static final int LOCAL_NODE_DESCRIPTORS_TYPE = 256;
    public static final int REMOTE_NODE_DESCRIPTORS_TYPE = 257;

    final SimpleNlriTypeRegistry tlvReg = SimpleNlriTypeRegistry.getInstance();

    @Override
    public void serializeTlvBody(final NodeIdentifier tlv, final ByteBuf body, final QName qName) {
        tlvReg.serializeTlv(AsNumTlvParser.AS_NUMBER_QNAME, tlv.getAsNumber(), body);
        tlvReg.serializeTlv(DomainIdTlvParser.DOMAIN_ID_QNAME, tlv.getDomainId(), body);
        tlvReg.serializeTlv(AreaIdTlvParser.AREA_ID_QNAME, tlv.getAreaId(), body);
        tlvReg.serializeTlv(CRouterIdentifier.QNAME, tlv.getCRouterIdentifier(), body);
        if (qName.equals(LocalNodeDescriptors.QNAME) || qName.equals(RemoteNodeDescriptors.QNAME)) {
            tlvReg.serializeTlv(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME, ((EpeNodeDescriptors)tlv).getBgpRouterId(), body);
            tlvReg.serializeTlv(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME, ((EpeNodeDescriptors)tlv).getMemberAsn(), body);
        }
    }

    @Override
    public NodeIdentifier parseTlvBody(final ByteBuf value) throws BGPParsingException {
        final AsNumber asNumber = tlvReg.parseSubTlv(value);
        final DomainIdentifier domainId = tlvReg.parseSubTlv(value);
        final AreaIdentifier areaId = tlvReg.parseSubTlv(value);
        final CRouterIdentifier routerId = tlvReg.parseSubTlv(value);

        return new NodeIdentifier() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return NodeIdentifier.class;
            }
            @Override
            public AsNumber getAsNumber() {
                return asNumber;
            }
            @Override
            public DomainIdentifier getDomainId() {
                return domainId;
            }
            @Override
            public AreaIdentifier getAreaId() {
                return areaId;
            }
            @Override
            public CRouterIdentifier getCRouterIdentifier() {
                return routerId;
            }
        };
    }

    @Override
    public NodeIdentifier parseEpeTlvBody(final int tlvType, final ByteBuf buffer, final NodeIdentifier descValue) throws BGPParsingException {
        final Ipv4Address bgpRId = tlvReg.parseSubTlv(buffer);
        final AsNumber memAsn = tlvReg.parseSubTlv(buffer);
        if (tlvType == LOCAL_NODE_DESCRIPTORS_TYPE) {
            final LocalNodeDescriptorsBuilder localBuilder = new LocalNodeDescriptorsBuilder(descValue).setBgpRouterId(bgpRId).setMemberAsn(memAsn);
            return localBuilder.build();
        } else {
            final RemoteNodeDescriptorsBuilder remBuilder = new RemoteNodeDescriptorsBuilder(descValue).setBgpRouterId(bgpRId).setMemberAsn(memAsn);
            return remBuilder.build();
        }
    }

}
