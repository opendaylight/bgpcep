/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class PrefixNlriParser {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixNlriParser.class);

    private PrefixNlriParser() {
        throw new UnsupportedOperationException();
    }

    /* Prefix Descriptor TLVs */
    private static final int OSPF_ROUTE_TYPE = 264;
    private static final int IP_REACHABILITY = 265;

    /* Prefix Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier OSPF_ROUTE_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern());
    @VisibleForTesting
    public static final NodeIdentifier IP_REACH_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ip-reachability-information").intern());

    static PrefixDescriptors parsePrefixDescriptors(final ByteBuf buffer, final boolean ipv4) throws BGPParsingException {
        final PrefixDescriptorsBuilder builder = new PrefixDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsing Prefix Descriptor: {}", ByteBufUtil.hexDump(value));
            }
            switch (type) {
            case TlvUtil.MULTI_TOPOLOGY_ID:
                final TopologyIdentifier topologyId = new TopologyIdentifier(value.readShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
                builder.setMultiTopologyId(topologyId);
                LOG.trace("Parsed Topology Identifier: {}", topologyId);
                break;
            case OSPF_ROUTE_TYPE:
                final int rt = value.readByte();
                final OspfRouteType routeType = OspfRouteType.forValue(rt);
                if (routeType == null) {
                    throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
                }
                builder.setOspfRouteType(routeType);
                LOG.trace("Parser RouteType: {}", routeType);
                break;
            case IP_REACHABILITY:
                final IpPrefix prefix = (ipv4) ? new IpPrefix(Ipv4Util.prefixForByteBuf(value)) : new IpPrefix(Ipv6Util.prefixForByteBuf(value));
                builder.setIpReachabilityInformation(prefix);
                LOG.trace("Parsed IP reachability info: {}", prefix);
                break;
            default:
                throw new BGPParsingException("Prefix Descriptor not recognized, type: " + type);
            }
        }
        LOG.debug("Finished parsing Prefix descriptors.");
        return builder.build();
    }

    static void serializePrefixDescriptors(final PrefixDescriptors descriptors, final ByteBuf buffer) {
        if (descriptors.getMultiTopologyId() != null) {
            TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(descriptors.getMultiTopologyId().getValue()), buffer);
        }
        if (descriptors.getOspfRouteType() != null) {
            TlvUtil.writeTLV(OSPF_ROUTE_TYPE,
                Unpooled.wrappedBuffer(new byte[] { UnsignedBytes.checkedCast(descriptors.getOspfRouteType().getIntValue()) }), buffer);
        }
        if (descriptors.getIpReachabilityInformation() != null) {
            final IpPrefix prefix = descriptors.getIpReachabilityInformation();
            byte[] prefixBytes = null;
            if (prefix.getIpv4Prefix() != null) {
                prefixBytes = Ipv4Util.bytesForPrefixBegin(prefix.getIpv4Prefix());
            } else if (prefix.getIpv6Prefix() != null) {
                prefixBytes = Ipv6Util.bytesForPrefixBegin(prefix.getIpv6Prefix());
            }
            TlvUtil.writeTLV(IP_REACHABILITY, Unpooled.wrappedBuffer(prefixBytes), buffer);
        }
    }

    // FIXME : use codec
    private static int domOspfRouteTypeValue(final String ospfRouteType) {
        switch (ospfRouteType) {
        case "intra-area":
            return OspfRouteType.IntraArea.getIntValue();
        case "inter-area":
            return OspfRouteType.InterArea.getIntValue();
        case "external1":
            return OspfRouteType.External1.getIntValue();
        case "external2":
            return OspfRouteType.External2.getIntValue();
        case "nssa1":
            return OspfRouteType.Nssa1.getIntValue();
        case "nssa2":
            return OspfRouteType.Nssa2.getIntValue();
        default:
            return 0;
        }
    }

    public static PrefixDescriptors serializePrefixDescriptors(final ContainerNode prefixDesc) {
        final PrefixDescriptorsBuilder prefixDescBuilder = new PrefixDescriptorsBuilder();
        if (prefixDesc.getChild(TlvUtil.MULTI_TOPOLOGY_NID).isPresent()) {
            prefixDescBuilder.setMultiTopologyId(new TopologyIdentifier((Integer) prefixDesc.getChild(TlvUtil.MULTI_TOPOLOGY_NID).get().getValue()));
        }
        final Optional<DataContainerChild<? extends PathArgument, ?>> ospfRoute = prefixDesc.getChild(OSPF_ROUTE_NID);
        if (ospfRoute.isPresent()) {
            prefixDescBuilder.setOspfRouteType(OspfRouteType.forValue(domOspfRouteTypeValue((String) ospfRoute.get().getValue())));
        }
        if (prefixDesc.getChild(IP_REACH_NID).isPresent()) {
            final String prefix = (String) prefixDesc.getChild(IP_REACH_NID).get().getValue();

            try {
                Ipv4Util.bytesForPrefixBegin(new Ipv4Prefix(prefix));
                prefixDescBuilder.setIpReachabilityInformation(new IpPrefix(new Ipv4Prefix(prefix)));
            } catch (final IllegalArgumentException e) {
                LOG.debug("Creating Ipv6 prefix because", e);
                prefixDescBuilder.setIpReachabilityInformation(new IpPrefix(new Ipv6Prefix(prefix)));
            }
        }
        return prefixDescBuilder.build();
    }
}
