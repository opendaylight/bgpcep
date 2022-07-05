/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4SourcePrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6SourcePrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;

final class FlowspecTypeRegistryBuilder {
    private final HandlerRegistry<DataContainer, FlowspecTypeParser, FlowspecTypeSerializer> handlers =
        new HandlerRegistry<>();

    FlowspecTypeRegistryBuilder() {
        final FSPortHandler portHandler = new FSPortHandler();
        registerFlowspecTypeParser(FSPortHandler.PORT_VALUE, portHandler);
        registerFlowspecTypeSerializer(PortCase.class, portHandler);

        final FSDestinationPortHandler destinationPortHandler = new FSDestinationPortHandler();
        registerFlowspecTypeParser(FSDestinationPortHandler.DESTINATION_PORT_VALUE, destinationPortHandler);
        registerFlowspecTypeSerializer(DestinationPortCase.class, destinationPortHandler);

        final FSSourcePortHandler sourcePortHandler = new FSSourcePortHandler();
        registerFlowspecTypeParser(FSSourcePortHandler.SOURCE_PORT_VALUE, sourcePortHandler);
        registerFlowspecTypeSerializer(SourcePortCase.class, sourcePortHandler);

        final FSIcmpTypeHandler icmpTypeHandler = new FSIcmpTypeHandler();
        registerFlowspecTypeParser(FSIcmpTypeHandler.ICMP_TYPE_VALUE, icmpTypeHandler);
        registerFlowspecTypeSerializer(IcmpTypeCase.class, icmpTypeHandler);

        final FSIcmpCodeHandler icmpCodeHandler = new FSIcmpCodeHandler();
        registerFlowspecTypeParser(FSIcmpCodeHandler.ICMP_CODE_VALUE, icmpCodeHandler);
        registerFlowspecTypeSerializer(IcmpCodeCase.class, icmpCodeHandler);

        final FSTcpFlagsHandler tcpFlagsHandler = new FSTcpFlagsHandler();
        registerFlowspecTypeParser(FSTcpFlagsHandler.TCP_FLAGS_VALUE, tcpFlagsHandler);
        registerFlowspecTypeSerializer(TcpFlagsCase.class, tcpFlagsHandler);

        final FSPacketLengthHandler packetlengthHandler = new FSPacketLengthHandler();
        registerFlowspecTypeParser(FSPacketLengthHandler.PACKET_LENGTH_VALUE, packetlengthHandler);
        registerFlowspecTypeSerializer(PacketLengthCase.class, packetlengthHandler);

        final FSDscpHandler dscpHandler = new FSDscpHandler();
        registerFlowspecTypeParser(FSDscpHandler.DSCP_VALUE, dscpHandler);
        registerFlowspecTypeSerializer(DscpCase.class, dscpHandler);
    }

    FlowspecTypeRegistryBuilder registerIpv4FlowspecTypeHandlers() {
        final FSIpv4DestinationPrefixHandler destinationPrefixHandler = new FSIpv4DestinationPrefixHandler();
        registerFlowspecTypeParser(FSIpv4DestinationPrefixHandler.DESTINATION_PREFIX_VALUE, destinationPrefixHandler);
        registerFlowspecTypeSerializer(DestinationPrefixCase.class, destinationPrefixHandler);

        final FSIpv4SourcePrefixHandler sourcePrefixHandler = new FSIpv4SourcePrefixHandler();
        registerFlowspecTypeParser(FSIpv4SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler);
        registerFlowspecTypeSerializer(SourcePrefixCase.class, sourcePrefixHandler);

        final FSIpProtocolHandler ipProtocolHandler = new FSIpProtocolHandler();
        registerFlowspecTypeParser(FSIpProtocolHandler.IP_PROTOCOL_VALUE, ipProtocolHandler);
        registerFlowspecTypeSerializer(ProtocolIpCase.class, ipProtocolHandler);

        final FSIpv4FragmentHandler fragmentHandler = new FSIpv4FragmentHandler();
        registerFlowspecTypeParser(FSIpv4FragmentHandler.FRAGMENT_VALUE, fragmentHandler);
        registerFlowspecTypeSerializer(FragmentCase.class, fragmentHandler);
        return this;
    }

    FlowspecTypeRegistryBuilder registerIpv6FlowspecTypeHandlers() {
        final FSIpv6DestinationPrefixHandler destinationPrefixHandler = new FSIpv6DestinationPrefixHandler();
        registerFlowspecTypeParser(FSIpv6DestinationPrefixHandler.IPV6_DESTINATION_PREFIX_VALUE,
            destinationPrefixHandler);
        registerFlowspecTypeSerializer(DestinationIpv6PrefixCase.class, destinationPrefixHandler);

        final FSIpv6SourcePrefixHandler sourcePrefixHandler = new FSIpv6SourcePrefixHandler();
        registerFlowspecTypeParser(FSIpv6SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler);
        registerFlowspecTypeSerializer(SourceIpv6PrefixCase.class, sourcePrefixHandler);

        final FSIpv6NextHeaderHandler nextHeaderHandler = new FSIpv6NextHeaderHandler();
        registerFlowspecTypeParser(FSIpv6NextHeaderHandler.NEXT_HEADER_VALUE, nextHeaderHandler);
        registerFlowspecTypeSerializer(NextHeaderCase.class, nextHeaderHandler);

        final FSIpv6FragmentHandler fragmentHandler = new FSIpv6FragmentHandler();
        registerFlowspecTypeParser(FSIpv6FragmentHandler.FRAGMENT_VALUE, fragmentHandler);
        registerFlowspecTypeSerializer(FragmentCase.class, fragmentHandler);

        final FSIpv6FlowLabelHandler flowlabelHandler = new FSIpv6FlowLabelHandler();
        registerFlowspecTypeParser(FSIpv6FlowLabelHandler.FLOW_LABEL_VALUE, flowlabelHandler);
        registerFlowspecTypeSerializer(FlowLabelCase.class, flowlabelHandler);
        return this;
    }

    public FlowspecTypeRegistry build() {
        return new FlowspecTypeRegistry(handlers);
    }

    private Registration registerFlowspecTypeParser(final int type, final FlowspecTypeParser parser) {
        return handlers.registerParser(type, parser);
    }

    private Registration registerFlowspecTypeSerializer(final Class<? extends FlowspecType> typeClass,
            final FlowspecTypeSerializer serializer) {
        return handlers.registerSerializer(typeClass, serializer);
    }
}
