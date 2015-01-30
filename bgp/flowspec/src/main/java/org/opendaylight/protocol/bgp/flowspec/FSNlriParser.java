/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.ComponentType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpCodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpTypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PacketLengthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.ProtocolIpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.TcpFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.port._case.destination.port.DestinationPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.port._case.destination.port.DestinationPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.prefix._case.DestinationPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.prefix._case.DestinationPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.dscp._case.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.dscp._case.DscpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.dscp._case.dscp.Dscps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.dscp._case.dscp.DscpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.fragment._case.fragment.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.fragment._case.fragment.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.code._case.IcmpCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.code._case.IcmpCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.code._case.icmp.code.Codes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.code._case.icmp.code.CodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.type._case.IcmpType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.type._case.IcmpTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.type._case.icmp.type.Types;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.type._case.icmp.type.TypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLength;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.packet.length._case.packet.length.PacketLengths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.packet.length._case.packet.length.PacketLengthsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.port.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.port.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.ProtocolIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.ProtocolIpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.protocol.ip.ProtocolIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.protocol.ip.ProtocolIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.port._case.source.port.SourcePorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.port._case.source.port.SourcePortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.prefix._case.SourcePrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.prefix._case.SourcePrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSNlriParser implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(FSNlriParser.class);

    private static final int NLRI_LENGTH = 1;
    private static final int NLRI_LENGTH_EXTENDED = 2;
    /**
     * Add this constant to length value to achieve all ones in the lefmost nibble.
     */
    private static final int LENGTH_MAGIC = 61440;

    private static final int END_OF_LIST = 7;
    private static final int AND_BIT = 6;
    private static final int LENGTH_BITMASK = 48;
    private static final int LENGTH_SHIFT = 4;
    private static final int LESS_THAN = 2;
    private static final int GREATER_THAN = 1;
    private static final int EQUAL = 0;

    private static final int NOT = 1;
    private static final int MATCH = 0;

    private static final int LAST_FRAGMENT = 7;
    private static final int FIRST_FRAGMENT = 6;
    private static final int IS_A_FRAGMENT = 5;
    private static final int DONT_FRAGMENT = 4;

    private static final int MAX_NLRI_LENGTH = 4095;
    private static final int MAX_NLRI_LENGTH_ONE_BYTE = 240;

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final PathAttributes pathAttributes = (PathAttributes) attribute;
        final PathAttributes1 pathAttributes1 = pathAttributes.getAugmentation(PathAttributes1.class);
        final PathAttributes2 pathAttributes2 = pathAttributes.getAugmentation(PathAttributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null &&
                routes.getDestinationType()
                instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase
                linkstateCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) routes.getDestinationType();
                serializeNlri(linkstateCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationFlowspecCase) {
                final DestinationFlowspecCase flowspecCase = (DestinationFlowspecCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
            }
        }
    }

    /**
     * Serializes Flowspec NLRI to ByteBuf.
     *
     * @param flow flowspec NLRI to be serialized
     */
    public static void serializeNlri(final List<Flowspec> flows, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final Flowspec flow : flows) {
            nlriByteBuf.writeByte(flow.getComponentType().getIntValue());
            final FlowspecType value = flow.getFlowspecType();
            switch (flow.getComponentType()) {
            case DestinationPrefix:
                nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((DestinationPrefixCase)value).getDestinationPrefix().getDestinationPrefix()));
                break;
            case SourcePrefix:
                nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((SourcePrefixCase)value).getSourcePrefix().getSourcePrefix()));
                break;
            case ProtocolIp:
                final List<ProtocolIps> ips = ((ProtocolIpCase)value).getProtocolIp().getProtocolIps();
                for (final ProtocolIps ip : ips) {
                    final ByteBuf protoBuf = Unpooled.buffer();
                    writeShortest(ip.getValue(), protoBuf);
                    serializeNumericOperand(ip.getOp(), protoBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(protoBuf);
                }
                break;
            case Port:
                final List<Ports> ports = ((PortCase)value).getPort().getPorts();
                for (final Ports port : ports) {
                    final ByteBuf portsBuf = Unpooled.buffer();
                    writeShortest(port.getValue(), portsBuf);
                    serializeNumericOperand(port.getOp(), portsBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(portsBuf);
                }
                break;
            case DestinationPort:
                final List<DestinationPorts> dPorts = ((DestinationPortCase)value).getDestinationPort().getDestinationPorts();
                for (final DestinationPorts port : dPorts) {
                    final ByteBuf portsBuf = Unpooled.buffer();
                    writeShortest(port.getValue(), portsBuf);
                    serializeNumericOperand(port.getOp(), portsBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(portsBuf);
                }
                break;
            case SourcePort:
                final List<SourcePorts> sPorts = ((SourcePortCase)value).getSourcePort().getSourcePorts();
                for (final SourcePorts port : sPorts) {
                    final ByteBuf portsBuf = Unpooled.buffer();
                    writeShortest(port.getValue(), portsBuf);
                    serializeNumericOperand(port.getOp(), portsBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(portsBuf);
                }
                break;
            case IcmpType:
                final List<Types> types = ((IcmpTypeCase)value).getIcmpType().getTypes();
                for (final Types type : types) {
                    serializeNumericOperand(type.getOp(), 1, nlriByteBuf);
                    writeShortest(type.getValue(), nlriByteBuf);
                }
                break;
            case IcmpCode:
                final List<Codes> codes = ((IcmpCodeCase)value).getIcmpCode().getCodes();
                for (final Codes code : codes) {
                    serializeNumericOperand(code.getOp(), 1, nlriByteBuf);
                    writeShortest(code.getValue(), nlriByteBuf);
                }
                break;
            case TcpFlags:
                final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.tcp.flags.TcpFlags> flags = ((TcpFlagsCase)value).getTcpFlags().getTcpFlags();
                for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.tcp.flags.TcpFlags flag : flags) {
                    final ByteBuf flagsBuf = Unpooled.buffer();
                    writeShortest(flag.getValue(), flagsBuf);
                    serializeBitmaskOperand(flag.getOp(), flagsBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(flagsBuf);
                }
                break;
            case PacketLength:
                final List<PacketLengths> lengths = ((PacketLengthCase)value).getPacketLength().getPacketLengths();
                for (final PacketLengths length : lengths) {
                    final ByteBuf lengthBuf = Unpooled.buffer();
                    writeShortest(length.getValue(), lengthBuf);
                    serializeNumericOperand(length.getOp(), lengthBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(lengthBuf);
                }
                break;
            case Dscp:
                final List<Dscps> dscps = ((DscpCase)value).getDscp().getDscps();
                for (final Dscps dscp : dscps) {
                    serializeNumericOperand(dscp.getOp(), 1, nlriByteBuf);
                    writeShortest(dscp.getValue().getValue(), nlriByteBuf);
                }
                break;
            case Fragment:
                final List<Fragments> fragments = ((FragmentCase)value).getFragment().getFragments();
                for (final Fragments fragment : fragments) {
                    serializeBitmaskOperand(fragment.getOp(), 1, nlriByteBuf);
                    nlriByteBuf.writeByte(serializeFragment(fragment.getValue()));
                }
                break;
            default:
                LOG.warn("Unknown Component Type.");
                break;
            }
        }
        Preconditions.checkState(nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH, "Maximum length of Flowspec NLRI reached.");
        if (nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH_ONE_BYTE) {
            buffer.writeByte(nlriByteBuf.readableBytes());
        } else {
            buffer.writeShort(nlriByteBuf.readableBytes() + LENGTH_MAGIC);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    /**
     * Given the integer values, this method instead of writing the value
     * in 4B field, compresses the value to lowest required byte field
     * depending on the value.
     *
     * @param value integer to be written
     * @param buffer ByteBuf where the value will be written
     */
    private static void writeShortest(final int value, final ByteBuf buffer) {
        if (value <= Values.UNSIGNED_BYTE_MAX_VALUE) {
            buffer.writeByte(UnsignedBytes.checkedCast(value));
        } else if (value <= Values.UNSIGNED_SHORT_MAX_VALUE) {
            ByteBufWriteUtil.writeUnsignedShort(value, buffer);
        } else if (value <= Values.UNSIGNED_INT_MAX_VALUE) {
            ByteBufWriteUtil.writeUnsignedInt(UnsignedInts.toLong(value), buffer);
        } else {
            buffer.writeLong(value);
        }
    }

    private static void serializeNumericOperand(final NumericOperand op, final int length, final ByteBuf buffer) {
        final BitSet bs = new BitSet(Byte.SIZE);
        if (op.isEndOfList() != null) {
            bs.set(END_OF_LIST, op.isEndOfList());
        }
        if (op.isAndBit() != null) {
            bs.set(AND_BIT, op.isAndBit());
        }
        if (op.isLessThan() != null) {
            bs.set(LESS_THAN, op.isLessThan());
        }
        if (op.isGreaterThan() != null) {
            bs.set(GREATER_THAN, op.isGreaterThan());
        }
        if (op.isEquals() != null) {
            bs.set(EQUAL, op.isEquals());
        }
        final byte len = (byte) (Integer.numberOfTrailingZeros(length) << LENGTH_SHIFT);
        buffer.writeByte(bs.toByteArray()[0] | len);
    }

    private static void serializeBitmaskOperand(final BitmaskOperand op, final int length, final ByteBuf buffer) {
        final BitSet bs = new BitSet(Byte.SIZE);
        if (op.isEndOfList() != null) {
            bs.set(END_OF_LIST, op.isEndOfList());
        }
        if (op.isAndBit() != null) {
            bs.set(AND_BIT, op.isAndBit());
        }
        if (op.isMatch() != null) {
            bs.set(MATCH, op.isMatch());
        }
        if (op.isNot() != null) {
            bs.set(NOT, op.isNot());
        }
        final byte len = (byte) (Integer.numberOfTrailingZeros(length) << LENGTH_SHIFT);
        buffer.writeByte(bs.toByteArray()[0] | len);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder().setDestinationFlowspec(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder().setFlowspec(
                    dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationFlowspecCaseBuilder().setDestinationFlowspec(new DestinationFlowspecBuilder().setFlowspec(dst).build()).build()).build());
    }

    public static List<Flowspec> parseNlri(final ByteBuf nlri) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<Flowspec> fss = new ArrayList<>();

        // length field can be one or two bytes (if needed)
        // check the length of nlri to see how many bytes we can skip
        final int length = nlri.readableBytes();
        nlri.skipBytes(length > 240 ? NLRI_LENGTH_EXTENDED : NLRI_LENGTH);

        while(nlri.isReadable()) {
            final FlowspecBuilder builder = new FlowspecBuilder();
            // read type
            final ComponentType type = ComponentType.forValue(nlri.readUnsignedByte());
            builder.setComponentType(type);
            switch (type) {
            case DestinationPrefix:
                builder.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(parseDestinationPrefix(nlri)).build());
                break;
            case SourcePrefix:
                builder.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(parseSourcePrefix(nlri)).build());
                break;
            case ProtocolIp:
                builder.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIp(parseProtocolIp(nlri)).build());
                break;
            case Port:
                builder.setFlowspecType(new PortCaseBuilder().setPort(parsePort(nlri)).build());
                break;
            case DestinationPort:
                builder.setFlowspecType(new DestinationPortCaseBuilder().setDestinationPort(parseDestinationPort(nlri)).build());
                break;
            case SourcePort:
                builder.setFlowspecType(new SourcePortCaseBuilder().setSourcePort(parseSourcePort(nlri)).build());
                break;
            case IcmpType:
                builder.setFlowspecType(new IcmpTypeCaseBuilder().setIcmpType(parseIcmpType(nlri)).build());
                break;
            case IcmpCode:
                builder.setFlowspecType(new IcmpCodeCaseBuilder().setIcmpCode(parseIcmpCode(nlri)).build());
                break;
            case TcpFlags:
                builder.setFlowspecType(new TcpFlagsCaseBuilder().setTcpFlags(parseTcpFlags(nlri)).build());
                break;
            case PacketLength:
                builder.setFlowspecType(new PacketLengthCaseBuilder().setPacketLength(parsePacketLength(nlri)).build());
                break;
            case Dscp:
                builder.setFlowspecType(new DscpCaseBuilder().setDscp(parseDscp(nlri)).build());
                break;
            case Fragment:
                builder.setFlowspecType(new FragmentCaseBuilder().setFragment(parseFragment(nlri)).build());
                break;
            default:
                break;
            }
            fss.add(builder.build());
        }
        return fss;
    }

    private static DestinationPrefix parseDestinationPrefix(final ByteBuf nlri) {
        return new DestinationPrefixBuilder().setDestinationPrefix(Ipv4Util.prefixForByteBuf(nlri)).build();
    }

    private static SourcePrefix parseSourcePrefix(final ByteBuf nlri) {
        return new SourcePrefixBuilder().setSourcePrefix(Ipv4Util.prefixForByteBuf(nlri)).build();
    }

    private static ProtocolIp parseProtocolIp(final ByteBuf nlri) {
        final List<ProtocolIps> ips = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final ProtocolIpsBuilder builder = new ProtocolIpsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ips.add(builder.build());
        }
        return new ProtocolIpBuilder().setProtocolIps(ips).build();
    }

    private static Port parsePort(final ByteBuf nlri) {
        final List<Ports> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final PortsBuilder builder = new PortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ports.add(builder.build());
        }
        return new PortBuilder().setPorts(ports).build();
    }

    private static DestinationPort parseDestinationPort(final ByteBuf nlri) {
        final List<DestinationPorts> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final DestinationPortsBuilder builder = new DestinationPortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ports.add(builder.build());
        }
        return new DestinationPortBuilder().setDestinationPorts(ports).build();
    }

    private static SourcePort parseSourcePort(final ByteBuf nlri) {
        final List<SourcePorts> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final SourcePortsBuilder builder = new SourcePortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ports.add(builder.build());
        }
        return new SourcePortBuilder().setSourcePorts(ports).build();
    }

    private static IcmpType parseIcmpType(final ByteBuf nlri) {
        final List<Types> types = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final TypesBuilder builder = new TypesBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            builder.setValue((short) UnsignedBytes.toInt((nlri.readByte())));
            end = op.isEndOfList();
            types.add(builder.build());
        }
        return new IcmpTypeBuilder().setTypes(types).build();
    }

    private static IcmpCode parseIcmpCode(final ByteBuf nlri) {
        final List<Codes> codes = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final CodesBuilder builder = new CodesBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            builder.setValue((short) UnsignedBytes.toInt((nlri.readByte())));
            end = op.isEndOfList();
            codes.add(builder.build());
        }
        return new IcmpCodeBuilder().setCodes(codes).build();
    }

    private static TcpFlags parseTcpFlags(final ByteBuf nlri) {
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.tcp.flags.TcpFlags> flags = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.tcp.flags.TcpFlagsBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.tcp.flags.TcpFlagsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final BitmaskOperand op = parseBitmask(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            flags.add(builder.build());
        }
        return new TcpFlagsBuilder().setTcpFlags(flags).build();
    }

    private static PacketLength parsePacketLength(final ByteBuf nlri) {
        final List<PacketLengths> plengths = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final PacketLengthsBuilder builder = new PacketLengthsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            // RFC does not specify which operand to use
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            plengths.add(builder.build());
        }
        return new PacketLengthBuilder().setPacketLengths(plengths).build();
    }

    private static Dscp parseDscp(final ByteBuf nlri) {
        final List<Dscps> dscps = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final DscpsBuilder builder = new DscpsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            // RFC does not specify operator
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            builder.setValue(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Dscp((short) UnsignedBytes.toInt((nlri.readByte()))));
            end = op.isEndOfList();
            dscps.add(builder.build());
        }
        return new DscpBuilder().setDscps(dscps).build();
    }

    private static Fragment parseFragment(final ByteBuf nlri) {
        final List<Fragments> fragments = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final FragmentsBuilder builder = new FragmentsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final BitmaskOperand op = parseBitmask(b);
            builder.setOp(op);
            builder.setValue(parseFragment(nlri.readByte()));
            end = op.isEndOfList();
            fragments.add(builder.build());
        }
        return new FragmentBuilder().setFragments(fragments).build();
    }

    private static NumericOperand parseNumeric(final byte op) {
        final BitSet bs = BitSet.valueOf(new long[] {UnsignedBytes.toInt(op)});
        return new NumericOperand(bs.get(AND_BIT), bs.get(END_OF_LIST), bs.get(EQUAL), bs.get(GREATER_THAN), bs.get(LESS_THAN));
    }

    private static BitmaskOperand parseBitmask(final byte op) {
        final BitSet bs = BitSet.valueOf(new long[] {UnsignedBytes.toInt(op)});
        return new BitmaskOperand(bs.get(AND_BIT), bs.get(END_OF_LIST), bs.get(MATCH), bs.get(NOT));
    }

    @VisibleForTesting
    public static short parseLength(final byte op) {
        return (short) (1 << ((op & LENGTH_BITMASK) >> LENGTH_SHIFT));
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment parseFragment(final byte fragment) {
        final BitSet bs = BitSet.valueOf(new long[] {UnsignedBytes.toInt(fragment)});
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment(bs.get(DONT_FRAGMENT), bs.get(FIRST_FRAGMENT), bs.get(IS_A_FRAGMENT), bs.get(LAST_FRAGMENT));
    }

    private static byte serializeFragment(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment fragment) {
        final BitSet bs = new BitSet(Byte.SIZE);
        if (fragment.isDoNot() != null) {
            bs.set(DONT_FRAGMENT, fragment.isDoNot());
        }
        if (fragment.isFirst() != null) {
            bs.set(FIRST_FRAGMENT, fragment.isFirst());
        }
        if (fragment.isIsA() != null) {
            bs.set(IS_A_FRAGMENT, fragment.isIsA());
        }
        if (fragment.isLast() != null) {
            bs.set(LAST_FRAGMENT, fragment.isLast());
        }
        return bs.toByteArray()[0];
    }
}
