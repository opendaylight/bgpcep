/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.flowspec.handlers.BitmaskOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericTwoByteOperandParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.DestinationPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.DscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.IcmpCodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.IcmpTypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.PacketLengthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.PortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.SourcePortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.TcpFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.dscp._case.Dscps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.dscp._case.DscpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.icmp.code._case.Codes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.icmp.code._case.CodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.icmp.type._case.Types;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.icmp.type._case.TypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengthsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.port._case.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.port._case.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFlowspecNlriParser implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowspecNlriParser.class);

    @VisibleForTesting
    static final NodeIdentifier FLOWSPEC_NID = new NodeIdentifier(Flowspec.QNAME);
    @VisibleForTesting
    static final NodeIdentifier FLOWSPEC_TYPE_NID = new NodeIdentifier(FlowspecType.QNAME);
    public static final NodeIdentifier DEST_PREFIX_NID =
        new NodeIdentifier(QName.create(DestinationPrefixCase.QNAME, "destination-prefix").intern());
    public static final NodeIdentifier SOURCE_PREFIX_NID =
        new NodeIdentifier(QName.create(SourcePrefixCase.QNAME, "source-prefix").intern());
    @VisibleForTesting
    static final NodeIdentifier PORTS_NID = new NodeIdentifier(Ports.QNAME);
    @VisibleForTesting
    static final NodeIdentifier DEST_PORT_NID = new NodeIdentifier(DestinationPorts.QNAME);
    @VisibleForTesting
    static final NodeIdentifier SOURCE_PORT_NID = new NodeIdentifier(SourcePorts.QNAME);
    @VisibleForTesting
    static final NodeIdentifier ICMP_TYPE_NID = new NodeIdentifier(Types.QNAME);
    @VisibleForTesting
    static final NodeIdentifier ICMP_CODE_NID = new NodeIdentifier(Codes.QNAME);
    @VisibleForTesting
    static final NodeIdentifier TCP_FLAGS_NID = new NodeIdentifier(TcpFlags.QNAME);
    @VisibleForTesting
    static final NodeIdentifier PACKET_LENGTHS_NID = new NodeIdentifier(PacketLengths.QNAME);
    @VisibleForTesting
    static final NodeIdentifier DSCP_NID = new NodeIdentifier(Dscps.QNAME);
    @VisibleForTesting
    static final NodeIdentifier FRAGMENT_NID = new NodeIdentifier(Fragments.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier OP_NID = new NodeIdentifier(QName.create(Flowspec.QNAME, "op").intern());
    @VisibleForTesting
    public static final NodeIdentifier VALUE_NID = new NodeIdentifier(QName.create(Flowspec.QNAME, "value").intern());

    protected final FlowspecTypeRegistry flowspecTypeRegistry;

    /**
     * Add this constant to length value to achieve all ones in the leftmost nibble.
     */
    private static final int LENGTH_MAGIC = 61440;
    private static final int MAX_NLRI_LENGTH = 0xFFF;
    private static final int MAX_NLRI_LENGTH_ONE_BYTE = 0xF0;

    @VisibleForTesting
    static final String DO_NOT_VALUE = "do-not";
    @VisibleForTesting
    static final String FIRST_VALUE = "first";
    @VisibleForTesting
    static final String LAST_VALUE = "last";
    @VisibleForTesting
    static final String IS_A_VALUE = "is-a";

    private static final String FLOW_SEPARATOR = " AND ";

    protected AbstractFlowspecNlriParser(final FlowspecTypeRegistry flowspecTypeRegistry) {
        this.flowspecTypeRegistry = requireNonNull(flowspecTypeRegistry);
    }

    protected abstract void serializeMpReachNlri(DestinationType dstType, ByteBuf byteAggregator);

    protected abstract void serializeMpUnreachNlri(DestinationType dstType, ByteBuf byteAggregator);

    public abstract void extractSpecificFlowspec(ChoiceNode fsType, FlowspecBuilder fsBuilder);

    protected abstract void stringSpecificFSNlriType(FlowspecType value, StringBuilder buffer);

    @Override
    public final void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final AttributesReach pathAttributes1 = pathAttributes.augmentation(AttributesReach.class);
        final AttributesUnreach pathAttributes2 = pathAttributes.augmentation(AttributesUnreach.class);

        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (routes != null) {
                serializeMpReachNlri(routes.getDestinationType(), byteAggregator);
            }
        }

        if (pathAttributes2 != null) {
            final WithdrawnRoutes routes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (routes != null) {
                serializeMpUnreachNlri(routes.getDestinationType(), byteAggregator);
            }
        }
    }

    protected final void serializeNlri(final List<Flowspec> flowspecList, final @NonNull ByteBuf buffer) {
        if (flowspecList != null) {
            for (final Flowspec flow : flowspecList) {
                flowspecTypeRegistry.serializeFlowspecType(flow.getFlowspecType(), buffer);
            }
        }
    }

    /**
     * Appends a Flowspec NLRI buffer to an output ByteBuf.
     *
     * @param pathId path ID
     * @param nlri NLRI to be appended
     * @param output where flowspec NLRI will be appended
     */
    protected static final void appendNlri(final @Nullable PathId pathId, final @NonNull ByteBuf nlri,
            final @NonNull ByteBuf output) {
        checkState(nlri.readableBytes() <= MAX_NLRI_LENGTH, "Maximum length of Flowspec NLRI reached.");
        PathIdUtil.writePathId(pathId, output);

        if (nlri.readableBytes() <= MAX_NLRI_LENGTH_ONE_BYTE) {
            output.writeByte(nlri.readableBytes());
        } else {
            output.writeShort(nlri.readableBytes() + LENGTH_MAGIC);
        }
        output.writeBytes(nlri);
    }

    public String stringNlri(final DataContainerNode flowspec) {
        return stringNlri(extractFlowspec(flowspec));
    }

    protected final String stringNlri(final List<Flowspec> flows) {
        final StringBuilder buffer = new StringBuilder("all packets ");
        final Joiner joiner = Joiner.on(FLOW_SEPARATOR);
        joiner.appendTo(buffer, flows.stream().map(this::encodeFlow).collect(Collectors.toList()));
        return buffer.toString().replace("  ", " ");
    }

    public final List<Flowspec> extractFlowspec(final DataContainerNode route) {
        requireNonNull(route, "Cannot extract flowspec from null route.d");
        final List<Flowspec> fsList = new ArrayList<>();
        final DataContainerChild flowspecs = route.childByArg(FLOWSPEC_NID);
        if (flowspecs != null) {
            for (final UnkeyedListEntryNode flowspec : ((UnkeyedListNode) flowspecs).body()) {
                final FlowspecBuilder fsBuilder = new FlowspecBuilder();
                final DataContainerChild flowspecType = flowspec.childByArg(FLOWSPEC_TYPE_NID);
                if (flowspecType != null) {
                    processFlowspecType((ChoiceNode) flowspecType, fsBuilder);
                }
                fsList.add(fsBuilder.build());
            }
        }
        return fsList;
    }

    private void processFlowspecType(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        final var common = processCommonFlowspec(fsType);
        if (common != null) {
            fsBuilder.setFlowspecType(common);
        } else {
            extractSpecificFlowspec(fsType, fsBuilder);
        }
    }

    private static @Nullable FlowspecType processCommonFlowspec(final ChoiceNode fsType) {
        final var ports = fsType.childByArg(PORTS_NID);
        if (ports != null) {
            return new PortCaseBuilder().setPorts(createPorts((UnkeyedListNode) ports)).build();
        }
        final var destPort = fsType.childByArg(DEST_PORT_NID);
        if (destPort != null) {
            return new DestinationPortCaseBuilder()
                .setDestinationPorts(createDestinationPorts((UnkeyedListNode) destPort))
                .build();
        }
        final var sourcePort = fsType.childByArg(SOURCE_PORT_NID);
        if (sourcePort != null) {
            return new SourcePortCaseBuilder().setSourcePorts(createSourcePorts((UnkeyedListNode) sourcePort)).build();
        }
        final var icmpType = fsType.childByArg(ICMP_TYPE_NID);
        if (icmpType != null) {
            return new IcmpTypeCaseBuilder().setTypes(createTypes((UnkeyedListNode) icmpType)).build();
        }
        final var icmpCode = fsType.childByArg(ICMP_CODE_NID);
        if (icmpCode != null) {
            return new IcmpCodeCaseBuilder().setCodes(createCodes((UnkeyedListNode) icmpCode)).build();
        }
        final var tcpFlags = fsType.childByArg(TCP_FLAGS_NID);
        if (tcpFlags != null) {
            return new TcpFlagsCaseBuilder().setTcpFlags(createTcpFlags((UnkeyedListNode) tcpFlags)).build();
        }
        final var packetLengths = fsType.childByArg(PACKET_LENGTHS_NID);
        if (packetLengths != null) {
            return new PacketLengthCaseBuilder()
                .setPacketLengths(createPacketLengths((UnkeyedListNode) packetLengths))
                .build();
        }
        final var dscp = fsType.childByArg(DSCP_NID);
        if (dscp != null) {
            return new DscpCaseBuilder().setDscps(createDscpsLengths((UnkeyedListNode) dscp)).build();
        }
        final var fragment = fsType.childByArg(FRAGMENT_NID);
        if (fragment != null) {
            return new FragmentCaseBuilder().setFragments(createFragments((UnkeyedListNode) fragment)).build();
        }
        return null;
    }

    private static List<Ports> createPorts(final UnkeyedListNode portsData) {
        return portsData.body().stream()
            .map(node -> {
                final var builder = new PortsBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint16) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<DestinationPorts> createDestinationPorts(final UnkeyedListNode destinationPortsData) {
        return destinationPortsData.body().stream()
            .map(node -> {
                final var builder = new DestinationPortsBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint16) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<SourcePorts> createSourcePorts(final UnkeyedListNode sourcePortsData) {
        return sourcePortsData.body().stream()
            .map(node -> {
                final var builder = new SourcePortsBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint16) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<Types> createTypes(final UnkeyedListNode typesData) {
        return typesData.body().stream()
            .map(node -> {
                final var builder = new TypesBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint8) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<Codes> createCodes(final UnkeyedListNode codesData) {
        return codesData.body().stream()
            .map(node -> {
                final var builder = new CodesBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint8) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<TcpFlags> createTcpFlags(final UnkeyedListNode tcpFlagsData) {
        return tcpFlagsData.body().stream()
            .map(node -> {
                final var builder = new TcpFlagsBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(BitmaskOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint16) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<PacketLengths> createPacketLengths(final UnkeyedListNode packetLengthsData) {
        return packetLengthsData.body().stream()
            .map(node -> {
                final var builder = new PacketLengthsBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint16) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<Dscps> createDscpsLengths(final UnkeyedListNode dscpLengthsData) {
        return dscpLengthsData.body().stream()
            .map(node -> {
                final var builder = new DscpsBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue(new Dscp((Uint8) value.body()));
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static List<Fragments> createFragments(final UnkeyedListNode fragmentsData) {
        return fragmentsData.body().stream()
            .map(node -> {
                final var builder = new FragmentsBuilder();
                final var op = node.childByArg(OP_NID);
                if (op != null) {
                    builder.setOp(BitmaskOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(VALUE_NID);
                if (value != null) {
                    builder.setValue(createFragment((Set<String>) value.body()));
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static Fragment createFragment(final Set<String> data) {
        return new Fragment(data.contains(DO_NOT_VALUE), data.contains(FIRST_VALUE), data.contains(IS_A_VALUE),
                data.contains(LAST_VALUE));
    }

    @VisibleForTesting
    final String encodeFlow(final Flowspec flow) {
        final StringBuilder buffer = new StringBuilder();
        final FlowspecType value = flow.getFlowspecType();
        if (value instanceof PortCase port) {
            buffer.append("where port ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(port.getPorts()));
        } else if (value instanceof DestinationPortCase destinationPort) {
            buffer.append("where destination port ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(destinationPort.getDestinationPorts()));
        } else if (value instanceof SourcePortCase sourcePort) {
            buffer.append("where source port ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(sourcePort.getSourcePorts()));
        } else if (value instanceof IcmpTypeCase icmpType) {
            buffer.append("where ICMP type ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(icmpType.getTypes()));
        } else if (value instanceof IcmpCodeCase icmpCode) {
            buffer.append("where ICMP code ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(icmpCode.getCodes()));
        } else if (value instanceof TcpFlagsCase tcpFlags) {
            buffer.append(stringTcpFlags(tcpFlags.getTcpFlags()));
        } else if (value instanceof PacketLengthCase packetLength) {
            buffer.append("where packet length ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(packetLength.getPacketLengths()));
        } else if (value instanceof DscpCase dscp) {
            buffer.append(stringDscp(dscp.getDscps()));
        } else if (value instanceof FragmentCase fragment) {
            buffer.append(stringFragment(fragment.getFragments()));
        } else {
            stringSpecificFSNlriType(value, buffer);
        }
        return buffer.toString();
    }

    private static String stringTcpFlags(final List<TcpFlags> flags) {
        final StringBuilder buffer = new StringBuilder("where TCP flags ");
        boolean isFirst = true;
        for (final TcpFlags item : flags) {
            buffer.append(BitmaskOperandParser.INSTANCE.toString(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringDscp(final List<Dscps> dscps) {
        final StringBuilder buffer = new StringBuilder("where DSCP ");
        boolean isFirst = true;
        for (final Dscps item : dscps) {
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(item.getOp(), isFirst));
            buffer.append(item.getValue().getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringFragment(final List<Fragments> fragments) {
        final StringBuilder buffer = new StringBuilder("where fragment ");
        boolean isFirst = true;
        for (final Fragments item : fragments) {
            buffer.append(BitmaskOperandParser.INSTANCE.toString(item.getOp(), isFirst));
            buffer.append(stringFragment(item.getValue()));
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringFragment(final Fragment fragment) {
        final StringBuilder buffer = new StringBuilder();
        if (fragment.getDoNot()) {
            buffer.append("'DO NOT' ");
        }
        if (fragment.getFirst()) {
            buffer.append("'IS FIRST' ");
        }
        if (fragment.getLast()) {
            buffer.append("'IS LAST' ");
        }
        if (fragment.getIsA()) {
            buffer.append("'IS A' ");
        }
        return buffer.toString();
    }

    public static int readNlriLength(final @NonNull ByteBuf nlri) {
        requireNonNull(nlri, "NLRI information cannot be null");
        checkState(nlri.isReadable(), "NLRI Byte buffer is not readable.");
        int length = nlri.readUnsignedByte();
        if (length >= MAX_NLRI_LENGTH_ONE_BYTE) {
            length = (length << Byte.SIZE | nlri.readUnsignedByte()) & MAX_NLRI_LENGTH;
        }
        checkState(length > 0 && length <= nlri.readableBytes(), "Invalid flowspec NLRI length %s", length);
        return length;
    }

    /**
     * Parses Flowspec NLRI into list of Flowspec.
     *
     * @param nlri byte representation of NLRI which will be parsed
     * @return list of Flowspec
     */
    // FIXME: throws BGPParsingException
    protected final List<Flowspec> parseNlriFlowspecList(final @NonNull ByteBuf nlri) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<Flowspec> fss = new ArrayList<>();

        while (nlri.isReadable()) {
            int nlriLength = readNlriLength(nlri);
            checkState(nlriLength > 0 && nlriLength <= nlri.readableBytes(),
                "Invalid flowspec NLRI length %s", nlriLength);
            LOG.trace("Flowspec NLRI length is {}", nlriLength);

            while (nlriLength > 0) {
                final int readableLength = nlri.readableBytes();
                final FlowspecBuilder builder = new FlowspecBuilder();
                builder.setFlowspecType(flowspecTypeRegistry.parseFlowspecType(nlri));
                fss.add(builder.build());
                final int flowspecTypeLength = readableLength - nlri.readableBytes();
                nlriLength -= flowspecTypeLength;
            }
            checkState(nlriLength == 0, "Remain NLRI length should be 0 instead of %s", nlriLength);
        }

        return fss;
    }

    @Override
    public final void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final PathId pathId = readPathId(nlri, builder.getAfi(), builder.getSafi(), constraint);
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder()
            .setDestinationType(parseAdvertizedNlri(nlri, pathId))
            .build());
    }

    @Override
    public final void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final PathId pathId = readPathId(nlri, builder.getAfi(), builder.getSafi(), constraint);
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder()
            .setDestinationType(parseWithdrawnNlri(nlri, pathId))
            .build()
        );
    }

    /**
     * Create advertized destination type.
     *
     * @param nlri   on-wire NLRI, with path ID already peeled
     * @param pathId associated path id with given NLRI
     * @return created destination type
     */
    protected abstract @NonNull DestinationType parseAdvertizedNlri(@NonNull ByteBuf nlri, @Nullable PathId pathId)
        throws BGPParsingException;

    /**
     * Parse and create withdrawn destination type.
     *
     * @param nlri   on-wire NLRI, with path ID already peeled
     * @param pathId associated path id with given NLRI
     * @return created destination type
     */
    protected abstract @NonNull DestinationType parseWithdrawnNlri(@NonNull ByteBuf nlri, @Nullable PathId pathId)
        throws BGPParsingException;

    protected static final @Nullable PathId readPathId(final @NonNull ByteBuf nlri, final AddressFamily afi,
            final SubsequentAddressFamily safi, final PeerSpecificParserConstraint constraint) {
        if (MultiPathSupportUtil.isTableTypeSupported(constraint, new BgpTableTypeImpl(afi, safi))) {
            return PathIdUtil.readPathId(nlri);
        }
        return null;
    }
}

