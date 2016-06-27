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
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrefixNlriSerializer implements NlriTypeCaseSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixNlriSerializer.class);


    /* Prefix Descriptor TLVs */
    private static final int OSPF_ROUTE_TYPE = 264;
    private static final int IP_REACHABILITY = 265;

    /* Node Descriptor Type */
    private static final int LOCAL_NODE_DESCRIPTORS_TYPE = 256;

    /* Prefix Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier OSPF_ROUTE_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern());
    @VisibleForTesting
    public static final NodeIdentifier IP_REACH_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ip-reachability-information").intern());

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

            final ByteBuf buf;
            if (prefix.getIpv4Prefix() != null) {
                buf = Unpooled.buffer(Ipv4Util.IP4_LENGTH + 1);
                ByteBufWriteUtil.writeMinimalPrefix(prefix.getIpv4Prefix(), buf);
            } else if (prefix.getIpv6Prefix() != null) {
                buf = Unpooled.buffer(Ipv6Util.IPV6_LENGTH + 1);
                ByteBufWriteUtil.writeMinimalPrefix(prefix.getIpv6Prefix(), buf);
            } else {
                buf = null;
            }
            TlvUtil.writeTLV(IP_REACHABILITY, buf, buffer);
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

    @Override
    public NlriType serializeTypeNlri(final CLinkstateDestination destination, final ByteBuf localdescs, final ByteBuf byteAggregator)  {
        final PrefixCase pCase = ((PrefixCase)destination.getObjectType());
        NodeNlriParser.serializeNodeIdentifier(pCase.getAdvertisingNodeDescriptors(), localdescs);
        TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS_TYPE, localdescs, byteAggregator);
        if (pCase.getPrefixDescriptors() != null) {
            serializePrefixDescriptors(pCase.getPrefixDescriptors(), byteAggregator);
            if (pCase.getPrefixDescriptors().getIpReachabilityInformation().getIpv4Prefix() != null) {
                return NlriType.Ipv4Prefix;
            } else {
                return NlriType.Ipv6Prefix;
            }
        }
        return null;
    }

}
