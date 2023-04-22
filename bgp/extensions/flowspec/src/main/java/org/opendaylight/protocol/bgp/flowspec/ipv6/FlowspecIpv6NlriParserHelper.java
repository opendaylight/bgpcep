/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.ipv6;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.next.header._case.NextHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.next.header._case.NextHeadersBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public final class FlowspecIpv6NlriParserHelper {
    private static final NodeIdentifier NEXT_HEADER_NID = new NodeIdentifier(NextHeaders.QNAME);
    private static final NodeIdentifier FLOW_LABEL_NID = new NodeIdentifier(FlowLabel.QNAME);

    private FlowspecIpv6NlriParserHelper() {
        // Hidden on purpose
    }

    public static void extractFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        final var destPrefix = fsType.childByArg(AbstractFlowspecNlriParser.DEST_PREFIX_NID);
        if (destPrefix != null) {
            fsBuilder.setFlowspecType(new DestinationIpv6PrefixCaseBuilder()
                .setDestinationPrefix(new Ipv6Prefix((String) destPrefix.body()))
                .build());
            return;
        }
        final var sourcePrefix = fsType.childByArg(AbstractFlowspecNlriParser.SOURCE_PREFIX_NID);
        if (sourcePrefix != null) {
            fsBuilder.setFlowspecType(new SourceIpv6PrefixCaseBuilder()
                .setSourcePrefix(new Ipv6Prefix((String) sourcePrefix.body()))
                .build());
            return;
        }
        final var nextHeader = fsType.childByArg(NEXT_HEADER_NID);
        if (nextHeader != null) {
            fsBuilder.setFlowspecType(new NextHeaderCaseBuilder()
                .setNextHeaders(createNextHeaders((UnkeyedListNode) nextHeader))
                .build());
            return;
        }
        final var flowLabel = fsType.childByArg(FLOW_LABEL_NID);
        if (flowLabel != null) {
            fsBuilder.setFlowspecType(new FlowLabelCaseBuilder()
                .setFlowLabel(createFlowLabels((UnkeyedListNode) flowLabel))
                .build());
        }
    }

    public static void buildFlowspecString(final FlowspecType value, final StringBuilder buffer) {
        if (value instanceof DestinationIpv6PrefixCase destinationIpv6) {
            buffer.append("to ").append(destinationIpv6.getDestinationPrefix().getValue());
        } else if (value instanceof SourceIpv6PrefixCase sourceIpv6) {
            buffer.append("from ").append(sourceIpv6.getSourcePrefix().getValue());
        } else if (value instanceof NextHeaderCase nextHeader) {
            buffer.append("where next header ").append(
                NumericOneByteOperandParser.INSTANCE.toString(nextHeader.getNextHeaders()));
        } else if (value instanceof FlowLabelCase flowLabel) {
            buffer.append("where flow label ").append(stringFlowLabel(flowLabel.getFlowLabel()));
        }
    }

    private static List<NextHeaders> createNextHeaders(final UnkeyedListNode nextHeadersData) {
        return nextHeadersData.body().stream()
            .map(node -> {
                final var builder = new NextHeadersBuilder();
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

    private static List<FlowLabel> createFlowLabels(final UnkeyedListNode flowLabelsData) {
        return flowLabelsData.body().stream()
            .map(node -> {
                final var builder = new FlowLabelBuilder();
                final var op = node.childByArg(AbstractFlowspecNlriParser.OP_NID);
                if (op != null) {
                    builder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) op.body()));
                }
                final var value = node.childByArg(AbstractFlowspecNlriParser.VALUE_NID);
                if (value != null) {
                    builder.setValue((Uint32) value.body());
                }
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    private static String stringFlowLabel(final List<FlowLabel> list) {
        final StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (var label : list) {
            sb.append(NumericOneByteOperandParser.INSTANCE.toString(label.getOp(), isFirst));
            sb.append(label.getValue());
            sb.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return sb.toString();
    }
}

