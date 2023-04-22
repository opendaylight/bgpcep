/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.ipv4;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.ProtocolIpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIpsBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

/**
 * Helper for parsing IPv4 Flowspec.
 *
 * @author Kevin Wang
 */
public final class FlowspecIpv4NlriParserHelper {
    private static final NodeIdentifier PROTOCOL_IP_NID = new NodeIdentifier(ProtocolIps.QNAME);

    private FlowspecIpv4NlriParserHelper() {
        // Hidden on purpose
    }

    public static void extractFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        final var destPrefix = fsType.childByArg(AbstractFlowspecNlriParser.DEST_PREFIX_NID);
        if (destPrefix != null) {
            fsBuilder.setFlowspecType(new DestinationPrefixCaseBuilder()
                .setDestinationPrefix(new Ipv4Prefix((String) destPrefix.body()))
                .build());
            return;
        }
        final var sourcePrefix = fsType.childByArg(AbstractFlowspecNlriParser.SOURCE_PREFIX_NID);
        if (sourcePrefix != null) {
            fsBuilder.setFlowspecType(new SourcePrefixCaseBuilder()
                .setSourcePrefix(new Ipv4Prefix((String) sourcePrefix.body()))
                .build());
            return;
        }
        final var protocolIp = fsType.childByArg(PROTOCOL_IP_NID);
        if (protocolIp != null) {
            fsBuilder.setFlowspecType(new ProtocolIpCaseBuilder()
                .setProtocolIps(createProtocolsIps((UnkeyedListNode) protocolIp))
                .build());
        }
    }

    public static void buildFlowspecString(final FlowspecType value, final StringBuilder buffer) {
        if (value instanceof DestinationPrefixCase destinationPrefix) {
            buffer.append("to ").append(destinationPrefix.getDestinationPrefix().getValue());
        } else if (value instanceof SourcePrefixCase sourcePrefix) {
            buffer.append("from ").append(sourcePrefix.getSourcePrefix().getValue());
        } else if (value instanceof ProtocolIpCase protocolIp) {
            buffer.append("where IP protocol ").append(
                NumericOneByteOperandParser.INSTANCE.toString(protocolIp.getProtocolIps()));
        }
    }

    private static List<ProtocolIps> createProtocolsIps(final UnkeyedListNode protocolIpsData) {
        return protocolIpsData.body().stream()
            .map(node -> {
                final var builder = new ProtocolIpsBuilder();
                final var op = node.childByArg(AbstractFlowspecNlriParser.OP_NID);
                if (op != null) {
                    builder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(AbstractFlowspecNlriParser.VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint8) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }
}

