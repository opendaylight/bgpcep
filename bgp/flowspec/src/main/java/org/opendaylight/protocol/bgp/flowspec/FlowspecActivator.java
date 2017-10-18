/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4SourcePrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6SourcePrefixHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowspecActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowspecActivator.class);

    private final SimpleFlowspecExtensionProviderContext context;
    private final List<AutoCloseable> registrations = new ArrayList<>();

    public SimpleFlowspecExtensionProviderContext getContext() {
        return this.context;
    }

    public FlowspecActivator(@Nonnull final SimpleFlowspecExtensionProviderContext context) {
        this.context = requireNonNull(context);

        for (SimpleFlowspecExtensionProviderContext.SAFI safi : SimpleFlowspecExtensionProviderContext.SAFI.values()) {
            registerCommonFlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                safi
            );
            registerIpv4FlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                safi
            );

            registerCommonFlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV6,
                safi
            );
            registerIpv6FlowspecTypeHandlers(
                SimpleFlowspecExtensionProviderContext.AFI.IPV6,
                safi
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
        final SimpleFlowspecExtensionProviderContext.SAFI safi
    ) {
        final SimpleFlowspecTypeRegistry flowspecTypeRegistry = this.context.getFlowspecTypeRegistry(afi, safi);

        final FSPortHandler portHandler = new FSPortHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSPortHandler.PORT_VALUE, portHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(PortCase.class, portHandler));

        final FSDestinationPortHandler destinationPortHandler = new FSDestinationPortHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSDestinationPortHandler.DESTINATION_PORT_VALUE, destinationPortHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DestinationPortCase.class, destinationPortHandler));

        final FSSourcePortHandler sourcePortHandler = new FSSourcePortHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSSourcePortHandler.SOURCE_PORT_VALUE, sourcePortHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(SourcePortCase.class, sourcePortHandler));

        final FSIcmpTypeHandler icmpTypeHandler = new FSIcmpTypeHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIcmpTypeHandler.ICMP_TYPE_VALUE, icmpTypeHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(IcmpTypeCase.class, icmpTypeHandler));

        final FSIcmpCodeHandler icmpCodeHandler = new FSIcmpCodeHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIcmpCodeHandler.ICMP_CODE_VALUE, icmpCodeHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(IcmpCodeCase.class, icmpCodeHandler));

        final FSTcpFlagsHandler tcpFlagsHandler = new FSTcpFlagsHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSTcpFlagsHandler.TCP_FLAGS_VALUE, tcpFlagsHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(TcpFlagsCase.class, tcpFlagsHandler));

        final FSPacketLengthHandler packetlengthHandler = new FSPacketLengthHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSPacketLengthHandler.PACKET_LENGTH_VALUE, packetlengthHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(PacketLengthCase.class, packetlengthHandler));

        final FSDscpHandler dscpHandler = new FSDscpHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSDscpHandler.DSCP_VALUE, dscpHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DscpCase.class, dscpHandler));
    }

    private void registerIpv4FlowspecTypeHandlers(
        final SimpleFlowspecExtensionProviderContext.AFI afi,
        final SimpleFlowspecExtensionProviderContext.SAFI safi
    ) {
        final SimpleFlowspecTypeRegistry flowspecTypeRegistry = this.context.getFlowspecTypeRegistry(afi, safi);

        final FSIpv4DestinationPrefixHandler destinationPrefixHandler = new FSIpv4DestinationPrefixHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv4DestinationPrefixHandler.DESTINATION_PREFIX_VALUE, destinationPrefixHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DestinationPrefixCase.class, destinationPrefixHandler));

        final FSIpv4SourcePrefixHandler sourcePrefixHandler = new FSIpv4SourcePrefixHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv4SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(SourcePrefixCase.class, sourcePrefixHandler));

        final FSIpProtocolHandler ipProtocolHandler = new FSIpProtocolHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpProtocolHandler.IP_PROTOCOL_VALUE, ipProtocolHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(ProtocolIpCase.class, ipProtocolHandler));

        final FSIpv4FragmentHandler fragmentHandler = new FSIpv4FragmentHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv4FragmentHandler.FRAGMENT_VALUE, fragmentHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(FragmentCase.class, fragmentHandler));
    }

    private void registerIpv6FlowspecTypeHandlers(
        final SimpleFlowspecExtensionProviderContext.AFI afi,
        final SimpleFlowspecExtensionProviderContext.SAFI safi
    ) {
        final SimpleFlowspecTypeRegistry flowspecTypeRegistry = this.context.getFlowspecTypeRegistry(afi, safi);

        final FSIpv6DestinationPrefixHandler destinationPrefixHandler = new FSIpv6DestinationPrefixHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6DestinationPrefixHandler.IPV6_DESTINATION_PREFIX_VALUE, destinationPrefixHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(DestinationIpv6PrefixCase.class, destinationPrefixHandler));

        final FSIpv6SourcePrefixHandler sourcePrefixHandler = new FSIpv6SourcePrefixHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(SourceIpv6PrefixCase.class, sourcePrefixHandler));

        final FSIpv6NextHeaderHandler nextHeaderHandler = new FSIpv6NextHeaderHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6NextHeaderHandler.NEXT_HEADER_VALUE, nextHeaderHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(NextHeaderCase.class, nextHeaderHandler));

        final FSIpv6FragmentHandler fragmentHandler = new FSIpv6FragmentHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6FragmentHandler.FRAGMENT_VALUE, fragmentHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(FragmentCase.class, fragmentHandler));

        final FSIpv6FlowLabelHandler flowlabelHandler = new FSIpv6FlowLabelHandler();
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeParser(FSIpv6FlowLabelHandler.FLOW_LABEL_VALUE, flowlabelHandler));
        this.registrations.add(flowspecTypeRegistry.registerFlowspecTypeSerializer(FlowLabelCase.class, flowlabelHandler));
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
