/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.OspfRouteTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.ReachTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractNlriTypeCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

abstract class AbstractPrefixNlriParser extends AbstractNlriTypeCodec {
    @Override
    protected final ObjectType parseObjectType(final ByteBuf buffer) {
        final NodeIdentifier oType = SimpleNlriTypeRegistry.getInstance().parseTlv(buffer);
        final PrefixCaseBuilder builder = new PrefixCaseBuilder();
        builder.setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder(oType).build());
        builder.setPrefixDescriptors(parsePrefixDescriptor(buffer));
        return builder.build();
    }

    @Override
    protected final void serializeObjectType(final ObjectType objectType, final ByteBuf buffer) {
        final PrefixCase prefix = (PrefixCase) objectType;
        SimpleNlriTypeRegistry.getInstance().serializeTlv(AdvertisingNodeDescriptors.QNAME, prefix.getAdvertisingNodeDescriptors(), buffer);
        serializePrefixDescriptor(prefix.getPrefixDescriptors(), buffer);
    }

    private static PrefixDescriptors parsePrefixDescriptor(final ByteBuf buffer) {
        final Map<QName, Object> tlvs = SimpleNlriTypeRegistry.getInstance().parseSubTlvs(buffer);
        final PrefixDescriptorsBuilder builder = new PrefixDescriptorsBuilder();
        builder.setMultiTopologyId((TopologyIdentifier) tlvs.get(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME));
        builder.setOspfRouteType((OspfRouteType) tlvs.get(OspfRouteTlvParser.OSPF_ROUTE_TYPE_QNAME));
        builder.setIpReachabilityInformation((IpPrefix) tlvs.get(ReachTlvParser.IP_REACHABILITY_QNAME));
        return builder.build();
    }

    private static void serializePrefixDescriptor(final PrefixDescriptors tlv, final ByteBuf buffer) {
        final SimpleNlriTypeRegistry reg = SimpleNlriTypeRegistry.getInstance();
        reg.serializeTlv(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME, tlv.getMultiTopologyId(), buffer);
        reg.serializeTlv(OspfRouteTlvParser.OSPF_ROUTE_TYPE_QNAME, tlv.getOspfRouteType(), buffer);
        reg.serializeTlv(ReachTlvParser.IP_REACHABILITY_QNAME, tlv.getIpReachabilityInformation(), buffer);
    }

    static PrefixDescriptors serializePrefixDescriptors(final ContainerNode prefixDesc) {
        final PrefixDescriptorsBuilder prefixDescBuilder = new PrefixDescriptorsBuilder();
        prefixDescBuilder.setMultiTopologyId(MultiTopoIdTlvParser.serializeModel(prefixDesc));
        prefixDescBuilder.setOspfRouteType(OspfRouteTlvParser.serializeModel(prefixDesc));
        prefixDescBuilder.setIpReachabilityInformation(ReachTlvParser.serializeModel(prefixDesc));
        return prefixDescBuilder.build();
    }
}
