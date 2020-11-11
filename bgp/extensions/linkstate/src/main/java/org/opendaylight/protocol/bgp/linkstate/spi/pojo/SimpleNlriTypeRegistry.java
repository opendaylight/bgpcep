/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.Ipv4PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.Ipv6PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.TeLspIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.TeLspIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AdvertisingNodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AreaIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.BgpRouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.DomainIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv4InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv4NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv6InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv6NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.LinkIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.LocalNodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.NodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.OspfRouteTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.ReachTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.RemoteNodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.RouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.NlriTypeCaseParser;
import org.opendaylight.protocol.bgp.linkstate.spi.NlriTypeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.concepts.MultiRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.TeLspCase;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleNlriTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);
    private static final @NonNull SimpleNlriTypeRegistry INSTANCE = new SimpleNlriTypeRegistry();

    private final HandlerRegistry<ObjectType, NlriTypeCaseParser, NlriTypeCaseSerializer> nlriRegistry =
            new HandlerRegistry<>();
    private final MultiRegistry<QName, LinkstateTlvParser.LinkstateTlvSerializer<?>> tlvSerializers =
            new MultiRegistry<>();
    private final MultiRegistry<Integer, LinkstateTlvParser<?>> tlvParsers = new MultiRegistry<>();

    private SimpleNlriTypeRegistry() {
        // NLRIs
        final NodeNlriParser nodeParser = new NodeNlriParser();
        nlriRegistry.registerParser(nodeParser.getNlriType(), nodeParser);
        nlriRegistry.registerSerializer(NodeCase.class, nodeParser);

        final LinkNlriParser linkParser = new LinkNlriParser();
        nlriRegistry.registerParser(linkParser.getNlriType(), linkParser);
        nlriRegistry.registerSerializer(LinkCase.class, linkParser);

        final Ipv4PrefixNlriParser ipv4PrefixParser = new Ipv4PrefixNlriParser();
        nlriRegistry.registerParser(ipv4PrefixParser.getNlriType(), ipv4PrefixParser);
        nlriRegistry.registerSerializer(PrefixCase.class, ipv4PrefixParser);
        final Ipv6PrefixNlriParser ipv6PrefixParser = new Ipv6PrefixNlriParser();
        nlriRegistry.registerParser(ipv6PrefixParser.getNlriType(), ipv6PrefixParser);

        final TeLspIpv4NlriParser teLspIpv4Parser = new TeLspIpv4NlriParser();
        nlriRegistry.registerParser(teLspIpv4Parser.getNlriType(), teLspIpv4Parser);
        nlriRegistry.registerSerializer(TeLspCase.class, teLspIpv4Parser);
        final TeLspIpv6NlriParser teLspIpv6Parser = new TeLspIpv6NlriParser();
        nlriRegistry.registerParser(teLspIpv6Parser.getNlriType(), teLspIpv6Parser);

        // TLVs
        final LocalNodeDescriptorTlvParser localParser = new LocalNodeDescriptorTlvParser();
        tlvParsers.register(localParser.getType(), localParser);
        tlvSerializers.register(localParser.getTlvQName(), localParser);

        final NodeDescriptorTlvParser nodeDescriptorParser = new NodeDescriptorTlvParser();
        tlvSerializers.register(nodeDescriptorParser.getTlvQName(), nodeDescriptorParser);

        final AdvertisingNodeDescriptorTlvParser advParser = new AdvertisingNodeDescriptorTlvParser();
        tlvSerializers.register(advParser.getTlvQName(), advParser);

        final RemoteNodeDescriptorTlvParser remoteParser = new RemoteNodeDescriptorTlvParser();
        tlvParsers.register(remoteParser.getType(), remoteParser);
        tlvSerializers.register(remoteParser.getTlvQName(), remoteParser);

        final RouterIdTlvParser bgpCrouterIdParser = new RouterIdTlvParser();
        tlvParsers.register(bgpCrouterIdParser.getType(), bgpCrouterIdParser);
        tlvSerializers.register(bgpCrouterIdParser.getTlvQName(), bgpCrouterIdParser);

        final AsNumTlvParser asNumParser = new AsNumTlvParser();
        tlvParsers.register(asNumParser.getType(), asNumParser);
        tlvSerializers.register(asNumParser.getTlvQName(), asNumParser);

        final DomainIdTlvParser bgpDomainIdParser = new DomainIdTlvParser();
        tlvParsers.register(bgpDomainIdParser.getType(), bgpDomainIdParser);
        tlvSerializers.register(bgpDomainIdParser.getTlvQName(), bgpDomainIdParser);

        final AreaIdTlvParser areaIdParser = new AreaIdTlvParser();
        tlvParsers.register(areaIdParser.getType(), areaIdParser);
        tlvSerializers.register(areaIdParser.getTlvQName(), areaIdParser);

        final BgpRouterIdTlvParser bgpRouterIdParser = new BgpRouterIdTlvParser();
        tlvParsers.register(bgpRouterIdParser.getType(), bgpRouterIdParser);
        tlvSerializers.register(bgpRouterIdParser.getTlvQName(), bgpRouterIdParser);

        final MemAsNumTlvParser memAsnParser = new MemAsNumTlvParser();
        tlvParsers.register(memAsnParser.getType(), memAsnParser);
        tlvSerializers.register(memAsnParser.getTlvQName(), memAsnParser);

        final LinkIdTlvParser linkIdParser = new LinkIdTlvParser();
        tlvParsers.register(linkIdParser.getType(), linkIdParser);
        tlvSerializers.register(linkIdParser.getTlvQName(), linkIdParser);

        final Ipv4NeighborTlvParser ipv4nNeighborParser = new Ipv4NeighborTlvParser();
        tlvParsers.register(ipv4nNeighborParser.getType(), ipv4nNeighborParser);
        tlvSerializers.register(ipv4nNeighborParser.getTlvQName(), ipv4nNeighborParser);

        final Ipv6NeighborTlvParser ipv6NeighborParser = new Ipv6NeighborTlvParser();
        tlvParsers.register(ipv6NeighborParser.getType(), ipv6NeighborParser);
        tlvSerializers.register(ipv6NeighborParser.getTlvQName(), ipv6NeighborParser);

        final Ipv4InterfaceTlvParser ipv4InterfaceParser = new Ipv4InterfaceTlvParser();
        tlvParsers.register(ipv4InterfaceParser.getType(), ipv4InterfaceParser);
        tlvSerializers.register(ipv4InterfaceParser.getTlvQName(), ipv4InterfaceParser);

        final Ipv6InterfaceTlvParser ipv6InterfaceParser = new Ipv6InterfaceTlvParser();
        tlvParsers.register(ipv6InterfaceParser.getType(), ipv6InterfaceParser);
        tlvSerializers.register(ipv6InterfaceParser.getTlvQName(), ipv6InterfaceParser);

        final MultiTopoIdTlvParser multiTopoIdParser = new MultiTopoIdTlvParser();
        tlvParsers.register(multiTopoIdParser.getType(), multiTopoIdParser);
        tlvSerializers.register(multiTopoIdParser.getTlvQName(), multiTopoIdParser);

        final ReachTlvParser ipv4ReachParser = new ReachTlvParser();
        tlvParsers.register(ipv4ReachParser.getType(), ipv4ReachParser);
        tlvSerializers.register(ipv4ReachParser.getTlvQName(), ipv4ReachParser);

        final OspfRouteTlvParser ospfRouterParser = new OspfRouteTlvParser();
        tlvParsers.register(ospfRouterParser.getType(), ospfRouterParser);
        tlvSerializers.register(ospfRouterParser.getTlvQName(), ospfRouterParser);
    }

    public static @NonNull SimpleNlriTypeRegistry getInstance() {
        return INSTANCE;
    }

    public CLinkstateDestination parseNlriType(final ByteBuf buffer) {
        final int type = buffer.readUnsignedShort();
        final int length = buffer.readUnsignedShort();
        final NlriTypeCaseParser parser = this.nlriRegistry.getParser(type);
        if (parser == null) {
            LOG.warn("Linkstate NLRI parser for Type: {} was not found.", type);
            return null;
        }
        return parser.parseTypeNlri(buffer.readSlice(length));
    }

    public void serializeNlriType(final CLinkstateDestination nlri, final ByteBuf byteAggregator) {
        if (nlri == null) {
            return;
        }
        requireNonNull(byteAggregator);
        final ObjectType objectType = nlri.getObjectType();
        final NlriTypeCaseSerializer serializer = this.nlriRegistry
                .getSerializer((Class<? extends ObjectType>) objectType.implementedInterface());
        if (serializer == null) {
            LOG.warn("Linkstate NLRI serializer for Type: {} was not found.", objectType.implementedInterface());
            return;
        }
        final ByteBuf nlriType = Unpooled.buffer();
        serializer.serializeTypeNlri(nlri, nlriType);
        TlvUtil.writeTLV(serializer.getNlriType(), nlriType, byteAggregator);
    }

    public <T> T parseTlv(final ByteBuf buffer) {
        return parseTlv(buffer, getParser(buffer));
    }

    private static <T> T parseTlv(final ByteBuf buffer, final LinkstateTlvParser<T> parser) {
        if (parser == null) {
            return null;
        }
        checkArgument(buffer != null && buffer.isReadable());
        final int length = buffer.readUnsignedShort();
        return parser.parseTlvBody(buffer.readSlice(length));
    }

    public Map<QName, Object> parseSubTlvs(final ByteBuf buffer) {
        final Map<QName, Object> tlvs = new HashMap<>();
        while (buffer.isReadable()) {
            final LinkstateTlvParser<?> tlvParser = getParser(buffer);
            final Object tlvBody = parseTlv(buffer, tlvParser);
            if (tlvBody != null) {
                tlvs.put(tlvParser.getTlvQName(), tlvBody);
            }
        }
        return tlvs;
    }

    private <T> LinkstateTlvParser<T> getParser(final ByteBuf buffer) {
        checkArgument(buffer != null && buffer.isReadable());
        final int type = buffer.readUnsignedShort();
        final LinkstateTlvParser<T> parser = (LinkstateTlvParser<T>) this.tlvParsers.get(type);
        if (parser == null) {
            LOG.warn("Linkstate TLV parser for Type: {} was not found.", type);
        }
        return parser;
    }

    public <T> void serializeTlv(final QName tlvQName, final T tlv, final ByteBuf buffer) {
        if (tlv == null) {
            return;
        }
        requireNonNull(tlvQName);
        requireNonNull(buffer);
        final LinkstateTlvParser.LinkstateTlvSerializer<T> tlvSerializer
                = (LinkstateTlvParser.LinkstateTlvSerializer<T>) this.tlvSerializers.get(tlvQName);
        if (tlvSerializer == null) {
            LOG.warn("Linkstate TLV serializer for QName: {} was not found.", tlvQName);
            return;
        }
        final ByteBuf body = Unpooled.buffer();
        tlvSerializer.serializeTlvBody(tlv, body);
        TlvUtil.writeTLV(tlvSerializer.getType(), body, buffer);
    }
}
