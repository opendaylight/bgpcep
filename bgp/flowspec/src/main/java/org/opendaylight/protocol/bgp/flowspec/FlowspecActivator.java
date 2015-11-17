/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.FSDestinationPortHandler;
import org.opendaylight.protocol.bgp.flowspec.FSDscpHandler;
import org.opendaylight.protocol.bgp.flowspec.FSIcmpCodeHandler;
import org.opendaylight.protocol.bgp.flowspec.FSIcmpTypeHandler;
import org.opendaylight.protocol.bgp.flowspec.FSIpProtocolHandler;
import org.opendaylight.protocol.bgp.flowspec.FSIpv6FlowLabelHandler;
import org.opendaylight.protocol.bgp.flowspec.FSIpv6NextHeaderHandler;
import org.opendaylight.protocol.bgp.flowspec.FSPacketLengthHandler;
import org.opendaylight.protocol.bgp.flowspec.FSPortHandler;
import org.opendaylight.protocol.bgp.flowspec.FSSourcePortHandler;
import org.opendaylight.protocol.bgp.flowspec.FSTcpFlagsHandler;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv4SourcePrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6DestinationPrefixHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6FragmentHandler;
import org.opendaylight.protocol.bgp.flowspec.handlers.FSIpv6SourcePrefixHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowspecActivator {
    private static final Logger LOG = LoggerFactory.getLogger(FlowspecActivator.class);

    private List<AutoCloseable> registrations;

    public void start(SimpleFlowspecExtensionProviderContext context) {
        this.registrations = Preconditions.checkNotNull(startImpl(context));
    }

    protected List<AutoCloseable> startImpl(final SimpleFlowspecExtensionProviderContext context) {

        final List<AutoCloseable> regs = new ArrayList<>();
        registerIpv4FlowspecTypeHandlers(regs, context);
        return regs;
    }

    protected void registerIpv4FlowspecTypeHandlers(final List<AutoCloseable> regs, SimpleFlowspecExtensionProviderContext context) {

        final FSIpv4DestinationPrefixHandler destinationPrefixHandler = new FSIpv4DestinationPrefixHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSIpv4DestinationPrefixHandler.DESTINATION_PREFIX_VALUE, destinationPrefixHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(DestinationPrefixCase.class, destinationPrefixHandler));

        final FSIpv4SourcePrefixHandler sourcePrefixHandler = new FSIpv4SourcePrefixHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSIpv4SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(SourcePrefixCase.class, sourcePrefixHandler));

        final FSIpProtocolHandler ipProtocolHandler = new FSIpProtocolHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSIpProtocolHandler.IP_PROTOCOL_VALUE, ipProtocolHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(ProtocolIpCase.class, ipProtocolHandler));

        final FSPortHandler portHandler = new FSPortHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSPortHandler.PORT_VALUE, portHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(PortCase.class, portHandler));

        final FSDestinationPortHandler destinationPortHandler = new FSDestinationPortHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSDestinationPortHandler.DESTINATION_PORT_VALUE, destinationPortHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(DestinationPortCase.class, destinationPortHandler));

        final FSSourcePortHandler sourcePortHandler = new FSSourcePortHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSSourcePortHandler.SOURCE_PORT_VALUE, sourcePortHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(SourcePortCase.class, sourcePortHandler));

        final FSIcmpTypeHandler icmpTypeHandler = new FSIcmpTypeHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSIcmpTypeHandler.ICMP_TYPE_VALUE, icmpTypeHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(IcmpTypeCase.class, icmpTypeHandler));

        final FSIcmpCodeHandler icmpCodeHandler = new FSIcmpCodeHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSIcmpCodeHandler.ICMP_CODE_VALUE, icmpCodeHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(IcmpCodeCase.class, icmpCodeHandler));

        final FSTcpFlagsHandler tcpFlagsHandler = new FSTcpFlagsHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSTcpFlagsHandler.TCP_FLAGS_VALUE, tcpFlagsHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(TcpFlagsCase.class, tcpFlagsHandler));

        final FSPacketLengthHandler packetlengthHandler = new FSPacketLengthHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSPacketLengthHandler.PACKET_LENGTH_VALUE, packetlengthHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(PacketLengthCase.class, packetlengthHandler));

        final FSDscpHandler dscpHandler = new FSDscpHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSDscpHandler.DSCP_VALUE, dscpHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(DscpCase.class, dscpHandler));

        final FSIpv4FragmentHandler fragmentHandler = new FSIpv4FragmentHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSIpv4FragmentHandler.FRAGMENT_VALUE, fragmentHandler));
        regs.add(context.registerFlowspecIpv4TypeSerializer(FragmentCase.class, fragmentHandler));
    }

    protected void registerIpv6FlowspecTypeHandlers(final List<AutoCloseable> regs, SimpleFlowspecExtensionProviderContext context) {

        final FSIpv6DestinationPrefixHandler destinationPrefixHandler = new FSIpv6DestinationPrefixHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSIpv6DestinationPrefixHandler.IPV6_DESTINATION_PREFIX_VALUE, destinationPrefixHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(DestinationIpv6PrefixCase.class, destinationPrefixHandler));

        final FSIpv6SourcePrefixHandler sourcePrefixHandler = new FSIpv6SourcePrefixHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSIpv6SourcePrefixHandler.SOURCE_PREFIX_VALUE, sourcePrefixHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(SourcePrefixCase.class, sourcePrefixHandler));

        final FSIpv6NextHeaderHandler ipProtocolHandler = new FSIpv6NextHeaderHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSIpv6NextHeaderHandler.NEXT_HEADER_VALUE, ipProtocolHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(NextHeaderCase.class, ipProtocolHandler));

        final FSPortHandler portHandler = new FSPortHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSPortHandler.PORT_VALUE, portHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(PortCase.class, portHandler));

        final FSDestinationPortHandler destinationPortHandler = new FSDestinationPortHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSDestinationPortHandler.DESTINATION_PORT_VALUE, destinationPortHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(DestinationPortCase.class, destinationPortHandler));

        final FSSourcePortHandler sourcePortHandler = new FSSourcePortHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSSourcePortHandler.SOURCE_PORT_VALUE, sourcePortHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(SourcePortCase.class, sourcePortHandler));

        final FSIcmpTypeHandler icmpTypeHandler = new FSIcmpTypeHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSIcmpTypeHandler.ICMP_TYPE_VALUE, icmpTypeHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(IcmpTypeCase.class, icmpTypeHandler));

        final FSIcmpCodeHandler icmpCodeHandler = new FSIcmpCodeHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSIcmpCodeHandler.ICMP_CODE_VALUE, icmpCodeHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(IcmpCodeCase.class, icmpCodeHandler));

        final FSTcpFlagsHandler tcpFlagsHandler = new FSTcpFlagsHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSTcpFlagsHandler.TCP_FLAGS_VALUE, tcpFlagsHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(TcpFlagsCase.class, tcpFlagsHandler));

        final FSPacketLengthHandler packetlengthHandler = new FSPacketLengthHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSPacketLengthHandler.PACKET_LENGTH_VALUE, packetlengthHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(PacketLengthCase.class, packetlengthHandler));

        final FSDscpHandler dscpHandler = new FSDscpHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSDscpHandler.DSCP_VALUE, dscpHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(DscpCase.class, dscpHandler));

        final FSIpv6FragmentHandler fragmentHandler = new FSIpv6FragmentHandler();
        regs.add(context.registerFlowspecIpv6TypeParser(FSIpv6FragmentHandler.FRAGMENT_VALUE, fragmentHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(FragmentCase.class, fragmentHandler));

        final FSIpv6FlowLabelHandler flowlabelHandler = new FSIpv6FlowLabelHandler();
        regs.add(context.registerFlowspecIpv4TypeParser(FSIpv6FlowLabelHandler.FLOW_LABEL_VALUE, flowlabelHandler));
        regs.add(context.registerFlowspecIpv6TypeSerializer(FlowLabelCase.class, flowlabelHandler));
    }

    public void stop() {
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
