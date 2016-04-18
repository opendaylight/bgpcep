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
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.linkstate.tlvs.IpReachTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LocalNodeDescriptorTlvC;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfRTypeTlvParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.PrefixIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class PrefixNlriParser implements NlriTypeCaseParser, NlriTypeCaseSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixNlriParser.class);

    /* Prefix Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier OSPF_ROUTE_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern());
    @VisibleForTesting
    public static final NodeIdentifier IP_REACH_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "ip-reachability-information").intern());

    final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();


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

    public PrefixIdentifiers parseTlvBody(final ByteBuf value, final boolean ipv4) throws BGPParsingException {
        //parse Prefix sub-TLVs
        final TopologyIdentifier topoId = nlriTypeReg.parseSubTlv(value);
        final OspfRouteType ospfRouteType = nlriTypeReg.parseSubTlv(value);
        final IpPrefix ipReach = IpReachTlvParser.parseTlvBody(value, ipv4);
        return new PrefixIdentifiers() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return PrefixIdentifiers.class;
            }
            @Override
            public TopologyIdentifier getMultiTopologyId() {
                return topoId;
            }
            @Override
            public OspfRouteType getOspfRouteType() {
                return ospfRouteType;
            }
            @Override
            public IpPrefix getIpReachabilityInformation() {
                return ipReach;
            }
        };
    }

    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localDescriptor, final ByteBuf restNlri) throws BGPParsingException {
        final PrefixCaseBuilder prefixBuilder = new PrefixCaseBuilder();
        final PrefixIdentifiers prefDesc;
        if (type.equals(NlriType.Ipv4Prefix)) {
            prefDesc = parseTlvBody(restNlri, true);
        } else {
            prefDesc = parseTlvBody(restNlri, false);
        }
        PrefixCase prefixcase = prefixBuilder.setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder(localDescriptor).build()).setPrefixDescriptors(new PrefixDescriptorsBuilder(prefDesc).build()).build();
        return prefixcase;
    }

    private void serializeTlvBody(final PrefixIdentifiers tlv, final ByteBuf body) {
        //serialize Prefix sub-TLVs
        nlriTypeReg.serializeTlv(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME, tlv.getMultiTopologyId(), body);
        nlriTypeReg.serializeTlv(OspfRTypeTlvParser.OSPF_ROUTE_TYPE_QNAME, tlv.getOspfRouteType(), body);
        nlriTypeReg.serializeTlv(IpReachTlvParser.IP_REACHABILITY_QNAME, tlv.getIpReachabilityInformation(), body);
    }

    @Override
    public NlriType serializeTypeNlri(final CLinkstateDestination destination, final ByteBuf localdescs, final ByteBuf byteAggregator)  {
        final PrefixCase pCase = ((PrefixCase) destination.getObjectType());
        nlriTypeReg.serializeTlv(AdvertisingNodeDescriptors.QNAME, pCase.getAdvertisingNodeDescriptors(), localdescs);
        TlvUtil.writeTLV(LocalNodeDescriptorTlvC.LOCAL_NODE_DESCRIPTORS_TYPE, localdescs, byteAggregator);
        if (pCase.getPrefixDescriptors() != null) {
            serializeTlvBody(pCase.getPrefixDescriptors(), byteAggregator);
            if (pCase.getPrefixDescriptors().getIpReachabilityInformation().getIpv4Prefix() != null) {
                return NlriType.Ipv4Prefix;
            } else {
                return NlriType.Ipv6Prefix;
            }
        }
        return null;
    }
}
