/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv4InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv4NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv6InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv6NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.LinkIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractNlriTypeCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkLrIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

public final class LinkNlriParser extends AbstractNlriTypeCodec {

    /* Link Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier IPV4_IFACE_NID = NodeIdentifier.create(
        Ipv4InterfaceTlvParser.IPV4_IFACE_ADDRESS_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier IPV4_NEIGHBOR_NID = NodeIdentifier.create(
        Ipv4NeighborTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier LINK_LOCAL_NID = NodeIdentifier.create(
        QName.create(LinkDescriptors.QNAME, "link-local-identifier").intern());
    @VisibleForTesting
    public static final NodeIdentifier LINK_REMOTE_NID = NodeIdentifier.create(
        QName.create(LinkDescriptors.QNAME, "link-remote-identifier").intern());
    private static final NodeIdentifier IPV6_IFACE_NID = NodeIdentifier.create(
        Ipv6InterfaceTlvParser.IPV6_IFACE_ADDRESS_QNAME);
    private static final NodeIdentifier IPV6_NEIGHBOR_NID = NodeIdentifier.create(
        Ipv6NeighborTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME);

    @FunctionalInterface
    private interface SerializerInterface {
        void check(Object cont);
    }

    static LinkDescriptors serializeLinkDescriptors(final ContainerNode descriptors) {
        final LinkDescriptorsBuilder linkDescBuilder = new LinkDescriptorsBuilder();

        final var descLocal = descriptors.childByArg(LINK_LOCAL_NID);
        final var descRemote = descriptors.childByArg(LINK_REMOTE_NID);
        if (descLocal != null && descRemote != null) {
            linkDescBuilder.setLinkLocalIdentifier((Uint32) descLocal.body());
            linkDescBuilder.setLinkRemoteIdentifier((Uint32) descRemote.body());
        }
        ifPresentApply(descriptors, IPV4_IFACE_NID,
            value -> linkDescBuilder.setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier((String) value)));
        ifPresentApply(descriptors, IPV6_IFACE_NID,
            value -> linkDescBuilder.setIpv6InterfaceAddress(new Ipv6InterfaceIdentifier((String) value)));
        ifPresentApply(descriptors, IPV4_NEIGHBOR_NID,
            value -> linkDescBuilder.setIpv4NeighborAddress(new Ipv4InterfaceIdentifier((String) value)));
        ifPresentApply(descriptors, IPV6_NEIGHBOR_NID,
            value -> linkDescBuilder.setIpv6NeighborAddress(new Ipv6InterfaceIdentifier((String) value)));
        ifPresentApply(descriptors, TlvUtil.MULTI_TOPOLOGY_NID,
            value -> linkDescBuilder.setMultiTopologyId(new TopologyIdentifier((Uint16) value)));
        return linkDescBuilder.build();
    }

    private static void ifPresentApply(final ContainerNode descriptors, final NodeIdentifier nid,
            final SerializerInterface serializer) {
        final DataContainerChild child = descriptors.childByArg(nid);
        if (child != null) {
            serializer.check(child.body());
        }
    }

    private static LinkDescriptors parseLinkDescriptor(final ByteBuf buffer) {
        final Map<QName, Object> tlvs = SimpleNlriTypeRegistry.getInstance().parseSubTlvs(buffer);
        final LinkDescriptorsBuilder builder = new LinkDescriptorsBuilder();

        final LinkLrIdentifiers linkIdentifiers = (LinkLrIdentifiers) tlvs.get(LinkIdTlvParser.QNAME);
        if (linkIdentifiers != null) {
            builder.setLinkLocalIdentifier(linkIdentifiers.getLinkLocalIdentifier());
            builder.setLinkRemoteIdentifier(linkIdentifiers.getLinkRemoteIdentifier());
        }
        builder.setIpv4InterfaceAddress(
            (Ipv4InterfaceIdentifier) tlvs.get(Ipv4InterfaceTlvParser.IPV4_IFACE_ADDRESS_QNAME));
        builder.setIpv4NeighborAddress(
            (Ipv4InterfaceIdentifier) tlvs.get(Ipv4NeighborTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME));
        builder.setIpv6InterfaceAddress(
            (Ipv6InterfaceIdentifier) tlvs.get(Ipv6InterfaceTlvParser.IPV6_IFACE_ADDRESS_QNAME));
        builder.setIpv6NeighborAddress(
            (Ipv6InterfaceIdentifier) tlvs.get(Ipv6NeighborTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME));
        builder.setMultiTopologyId((TopologyIdentifier) tlvs.get(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME));

        return builder.build();
    }

    @Override
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final SimpleNlriTypeRegistry reg = SimpleNlriTypeRegistry.getInstance();
        final LocalNodeDescriptors localDescriptor = reg.parseTlv(buffer);
        final RemoteNodeDescriptors remoteDescriptor = reg.parseTlv(buffer);
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
        final SimpleNlriTypeRegistry reg = SimpleNlriTypeRegistry.getInstance();
        reg.serializeTlv(LocalNodeDescriptors.QNAME, linkNlri.getLocalNodeDescriptors(), buffer);
        reg.serializeTlv(RemoteNodeDescriptors.QNAME, linkNlri.getRemoteNodeDescriptors(), buffer);
        serializeLinkDescriptor(linkNlri.getLinkDescriptors(), buffer);
    }

    private static void serializeLinkDescriptor(final LinkDescriptors linkDescriptor, final ByteBuf body) {
        final SimpleNlriTypeRegistry reg = SimpleNlriTypeRegistry.getInstance();
        if (linkDescriptor.getLinkLocalIdentifier() != null && linkDescriptor.getLinkRemoteIdentifier() != null) {
            reg.serializeTlv(LinkIdTlvParser.QNAME, linkDescriptor, body);
        }
        reg.serializeTlv(Ipv4InterfaceTlvParser.IPV4_IFACE_ADDRESS_QNAME, linkDescriptor.getIpv4InterfaceAddress(),
            body);
        reg.serializeTlv(Ipv4NeighborTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME, linkDescriptor.getIpv4NeighborAddress(),
            body);
        reg.serializeTlv(Ipv6InterfaceTlvParser.IPV6_IFACE_ADDRESS_QNAME, linkDescriptor.getIpv6InterfaceAddress(),
            body);
        reg.serializeTlv(Ipv6NeighborTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME, linkDescriptor.getIpv6NeighborAddress(),
            body);
        reg.serializeTlv(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME, linkDescriptor.getMultiTopologyId(), body);
    }

    @Override
    public int getNlriType() {
        return NlriType.Link.getIntValue();
    }
}
