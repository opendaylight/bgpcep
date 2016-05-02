/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6SourcePrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4SourcePrefixHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv4.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowspecActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowspecActivator.class);

    private final List<AutoCloseable> registrations = new ArrayList<>();

    public FlowspecActivator(final SimpleFlowspecExtensionProviderContext context) {
        for (SimpleFlowspecExtensionProviderContext.SAFI safi : SimpleFlowspecExtensionProviderContext.SAFI.values()) {
            registerCommonFlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                safi,
                registrations,
                context
            );
            registerIpv4FlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                safi,
                registrations,
                context
            );

            registerCommonFlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV6,
                safi,
                registrations,
                context
            );
            registerIpv6FlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV6,
                safi,
                registrations,
                context
            );
        }
    }

    /**
     * Register the common flowspec type handlers
     *
     * @param afi
     * @param safi
     */
    private void registerCommonFlowspecTypeHandlers(
        final SimpleFlowspecExtensionProviderContext.AFI afi,
        final SimpleFlowspecExtensionProviderContext.SAFI safi,
        final List<AutoCloseable> regs,
        final SimpleFlowspecExtensionProviderContext context
    ) {
        final SimpleFlowspecTypeRegistry flowspecTypeRegistry = context.getFlowspecTypeRegistry(afi, safi);

        final FSPortHandler portHandler = new FSPortHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSPortHandler.PORT_VALUE, portHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(PortCase.class, portHandler));

        final FSDestinationPortHandler destinationPortHandler = new FSDestinationPortHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSDestinationPortHandler.DESTINATION_PORT_VALUE, destinationPortHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DestinationPortCase.class, destinationPortHandler));

        final FSSourcePortHandler sourcePortHandler = new FSSourcePortHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSSourcePortHandler.SOURCE_PORT_VALUE, sourcePortHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(SourcePortCase.class, sourcePortHandler));

        final FSIcmpTypeHandler icmpTypeHandler = new FSIcmpTypeHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIcmpTypeHandler.ICMP_TYPE_VALUE, icmpTypeHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(IcmpTypeCase.class, icmpTypeHandler));

        final FSIcmpCodeHandler icmpCodeHandler = new FSIcmpCodeHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIcmpCodeHandler.ICMP_CODE_VALUE, icmpCodeHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(IcmpCodeCase.class, icmpCodeHandler));

        final FSTcpFlagsHandler tcpFlagsHandler = new FSTcpFlagsHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSTcpFlagsHandler.TCP_FLAGS_VALUE, tcpFlagsHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(TcpFlagsCase.class, tcpFlagsHandler));

        final FSPacketLengthHandler packetlengthHandler = new FSPacketLengthHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSPacketLengthHandler.PACKET_LENGTH_VALUE, packetlengthHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(PacketLengthCase.class, packetlengthHandler));

        final FSDscpHandler dscpHandler = new FSDscpHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSDscpHandler.DSCP_VALUE, dscpHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DscpCase.class, dscpHandler));
    }

    protected void registerIpv4FlowspecTypeHandlers(
        final SimpleFlowspecExtensionProviderContext.AFI afi,
        final SimpleFlowspecExtensionProviderContext.SAFI safi,
        final List<AutoCloseable> regs,
        final SimpleFlowspecExtensionProviderContext context
    ) {
        final SimpleFlowspecTypeRegistry flowspecTypeRegistry = context.getFlowspecTypeRegistry(afi, safi);

        final FSIpv4DestinationPrefixHandler destinationPrefixHandler = new FSIpv4DestinationPrefixHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv4DestinationPrefixHandler.DESTINATION_PREFIX_VALUE, destinationPrefixHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DestinationPrefixCase.class, destinationPrefixHandler));

        final FSIpv4SourcePrefixHandler sourcePrefixHandler = new FSIpv4SourcePrefixHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv4SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(SourcePrefixCase.class, sourcePrefixHandler));

        final FSIpProtocolHandler ipProtocolHandler = new FSIpProtocolHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpProtocolHandler.IP_PROTOCOL_VALUE, ipProtocolHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(ProtocolIpCase.class, ipProtocolHandler));

        final FSIpv4FragmentHandler fragmentHandler = new FSIpv4FragmentHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv4FragmentHandler.FRAGMENT_VALUE, fragmentHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(FragmentCase.class, fragmentHandler));
    }

    protected void registerIpv6FlowspecTypeHandlers(
        final SimpleFlowspecExtensionProviderContext.AFI afi,
        final SimpleFlowspecExtensionProviderContext.SAFI safi,
        final List<AutoCloseable> regs,
        final SimpleFlowspecExtensionProviderContext context
    ) {
        final SimpleFlowspecTypeRegistry flowspecTypeRegistry = context.getFlowspecTypeRegistry(afi, safi);

        final FSIpv6DestinationPrefixHandler destinationPrefixHandler = new FSIpv6DestinationPrefixHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6DestinationPrefixHandler.IPV6_DESTINATION_PREFIX_VALUE, destinationPrefixHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DestinationIpv6PrefixCase.class, destinationPrefixHandler));

        final FSIpv6SourcePrefixHandler sourcePrefixHandler = new FSIpv6SourcePrefixHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(SourceIpv6PrefixCase.class, sourcePrefixHandler));

        final FSIpv6NextHeaderHandler nextHeaderHandler = new FSIpv6NextHeaderHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6NextHeaderHandler.NEXT_HEADER_VALUE, nextHeaderHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(NextHeaderCase.class, nextHeaderHandler));

        final FSIpv6FragmentHandler fragmentHandler = new FSIpv6FragmentHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6FragmentHandler.FRAGMENT_VALUE, fragmentHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(FragmentCase.class, fragmentHandler));

        final FSIpv6FlowLabelHandler flowlabelHandler = new FSIpv6FlowLabelHandler();
        regs.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6FlowLabelHandler.FLOW_LABEL_VALUE, flowlabelHandler));
        regs.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(FlowLabelCase.class, flowlabelHandler));
    }

    @Override
    public void close() {
        if (this.registrations == null) {
            return;
        }
        for (final AutoCloseable r : this.registrations) {
            try {
                r.close();
            } catch (final Exception e) {
                LOG.warn("Failed to close registration", e);
            }
        }
    }
}
