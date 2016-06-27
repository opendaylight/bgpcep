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
import io.netty.buffer.ByteBufUtil;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class PrefixIpv6NlriParser implements NlriTypeCaseParser {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixIpv6NlriParser.class);

    /* Prefix Descriptor TLVs */
    private static final int OSPF_ROUTE_TYPE = 264;
    private static final int IP_REACHABILITY = 265;

    /* Prefix Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier OSPF_ROUTE_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern());
    @VisibleForTesting
    public static final NodeIdentifier IP_REACH_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ip-reachability-information").intern());

    static PrefixDescriptors parseIpv6PrefixDescriptors(final ByteBuf buffer) throws BGPParsingException {
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
                final IpPrefix prefix = new IpPrefix(Ipv6Util.prefixForByteBuf(value));
                builder.setIpReachabilityInformation(prefix);
                LOG.trace("Parsed IP reachability info: {}", prefix);
                break;
            default:
                throw new BGPParsingException("Prefix Descriptor not recognized, type: " + type);
            }
        }
        LOG.debug("Finished parsing Ipv6 Prefix descriptors.");
        return builder.build();
    }

    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localdescriptor, final ByteBuf restNlri) throws BGPParsingException {
        PrefixCaseBuilder prefixbuilder = new PrefixCaseBuilder();
        PrefixDescriptors prefdesc = parseIpv6PrefixDescriptors(restNlri);
        PrefixCase prefixcase = prefixbuilder.setAdvertisingNodeDescriptors((AdvertisingNodeDescriptors) localdescriptor).setPrefixDescriptors(prefdesc).build();
        return prefixcase;
    }

}
