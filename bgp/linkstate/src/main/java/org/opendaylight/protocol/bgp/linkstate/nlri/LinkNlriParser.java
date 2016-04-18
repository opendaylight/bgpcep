/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv6InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv6NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkLrIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class LinkNlriParser extends AbstractNlriTypeCodec {

    /* Link Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier IPV4_IFACE_NID = new NodeIdentifier(Ipv4InterfaceTlvParser.IPV4_IFACE_ADDRESS_QNAME);
    private static final NodeIdentifier IPV6_IFACE_NID = new NodeIdentifier(Ipv6InterfaceTlvParser.IPV6_IFACE_ADDRESS_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier IPV4_NEIGHBOR_NID = new NodeIdentifier(Ipv4NeighborTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME);
    private static final NodeIdentifier IPV6_NEIGHBOR_NID = new NodeIdentifier(Ipv6NeighborTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier LINK_LOCAL_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "link-local-identifier").intern());
    @VisibleForTesting
    public static final NodeIdentifier LINK_REMOTE_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "link-remote-identifier").intern());

    public LinkNlriParser(final SimpleNlriTypeRegistry registry) {
        super(registry);
    }

    static LinkDescriptors serializeLinkDescriptors(final ContainerNode descriptors) {
        final LinkDescriptorsBuilder linkDescBuilder = new LinkDescriptorsBuilder();

        if (descriptors.getChild(LINK_LOCAL_NID).isPresent() && descriptors.getChild(LINK_REMOTE_NID).isPresent()) {
            linkDescBuilder.setLinkLocalIdentifier((Long) descriptors.getChild(LINK_LOCAL_NID).get().getValue());
            linkDescBuilder.setLinkRemoteIdentifier((Long) descriptors.getChild(LINK_REMOTE_NID).get().getValue());
        }
        if (descriptors.getChild(IPV4_IFACE_NID).isPresent()) {
            linkDescBuilder.setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier((String) descriptors.getChild(IPV4_IFACE_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV6_IFACE_NID).isPresent()) {
            linkDescBuilder.setIpv6InterfaceAddress(new Ipv6InterfaceIdentifier((String) descriptors.getChild(IPV6_IFACE_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV4_NEIGHBOR_NID).isPresent()) {
            linkDescBuilder.setIpv4NeighborAddress(new Ipv4InterfaceIdentifier((String) descriptors.getChild(IPV4_NEIGHBOR_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV6_NEIGHBOR_NID).isPresent()) {
            linkDescBuilder.setIpv6NeighborAddress(new Ipv6InterfaceIdentifier((String) descriptors.getChild(IPV6_NEIGHBOR_NID).get().getValue()));
        }
        if (descriptors.getChild(TlvUtil.MULTI_TOPOLOGY_NID).isPresent()) {
            linkDescBuilder.setMultiTopologyId(new TopologyIdentifier((Integer) descriptors.getChild(TlvUtil.MULTI_TOPOLOGY_NID).get().getValue()));
        }
        return linkDescBuilder.build();
    }

    private LinkDescriptors parseLinkDescriptor(final ByteBuf buffer) {
        final Map<QName, Object> tlvs = this.registry.parseSubTlvs(buffer);
        final LinkDescriptorsBuilder builder = new LinkDescriptorsBuilder();

        final LinkLrIdentifiers linkIdentifiers = (LinkLrIdentifiers) tlvs.get(LinkLrIdentifiers.QNAME);
        if (linkIdentifiers != null) {
            builder.setLinkLocalIdentifier(linkIdentifiers.getLinkLocalIdentifier());
            builder.setLinkRemoteIdentifier(linkIdentifiers.getLinkRemoteIdentifier());
        }
        builder.setIpv4InterfaceAddress((Ipv4InterfaceIdentifier) tlvs.get(Ipv4InterfaceTlvParser.IPV4_IFACE_ADDRESS_QNAME));
        builder.setIpv4NeighborAddress((Ipv4InterfaceIdentifier) tlvs.get(Ipv4NeighborTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME));
        builder.setIpv6InterfaceAddress((Ipv6InterfaceIdentifier) tlvs.get(Ipv6InterfaceTlvParser.IPV6_IFACE_ADDRESS_QNAME));
        builder.setIpv6NeighborAddress((Ipv6InterfaceIdentifier) tlvs.get(Ipv6NeighborTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME));
        builder.setMultiTopologyId((TopologyIdentifier) tlvs.get(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME));

        return builder.build();
    }

    @Override
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final LocalNodeDescriptors localDescriptor = this.registry.parseTlv(buffer);
        final RemoteNodeDescriptors remoteDescriptor = this.registry.parseTlv(buffer);
        final LinkDescriptors linkDescriptor = parseLinkDescriptor(buffer);
        return new LinkCaseBuilder()
            .setLinkDescriptors(linkDescriptor)
            .setLocalNodeDescriptors(localDescriptor)
            .setRemoteNodeDescriptors(remoteDescriptor)
            .build();
    }

    @Override
    protected void serializeObjectType(final ObjectType objectType, final ByteBuf buffer) {
        final LinkCase linkNlri = (LinkCase) objectType;
        this.registry.serializeTlv(LocalNodeDescriptors.QNAME, linkNlri.getLocalNodeDescriptors(), buffer);
        this.registry.serializeTlv(RemoteNodeDescriptors.QNAME, linkNlri.getRemoteNodeDescriptors(), buffer);
        serializeLinkDescriptor(linkNlri.getLinkDescriptors(), buffer);
    }

    private void serializeLinkDescriptor(final LinkDescriptors linkDescriptor, final ByteBuf body) {
        if (linkDescriptor.getLinkLocalIdentifier() != null && linkDescriptor.getLinkRemoteIdentifier() != null) {
            this.registry.serializeTlv(LinkLrIdentifiers.QNAME, linkDescriptor, body);
        }
        this.registry.serializeTlv(Ipv4InterfaceTlvParser.IPV4_IFACE_ADDRESS_QNAME, linkDescriptor.getIpv4InterfaceAddress(), body);
        this.registry.serializeTlv(Ipv4NeighborTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME, linkDescriptor.getIpv4NeighborAddress(), body);
        this.registry.serializeTlv(Ipv6InterfaceTlvParser.IPV6_IFACE_ADDRESS_QNAME, linkDescriptor.getIpv6InterfaceAddress(), body);
        this.registry.serializeTlv(Ipv6NeighborTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME, linkDescriptor.getIpv6NeighborAddress(), body);
        this.registry.serializeTlv(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME, linkDescriptor.getMultiTopologyId(), body);
    }

    @Override
    public int getNlriType() {
        return NlriType.Link.getIntValue();
    }
}
