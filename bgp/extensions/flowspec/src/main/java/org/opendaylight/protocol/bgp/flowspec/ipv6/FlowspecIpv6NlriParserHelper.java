/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.ipv6;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public final class FlowspecIpv6NlriParserHelper {
    private static final NodeIdentifier NEXT_HEADER_NID = new NodeIdentifier(NextHeaders.QNAME);
    private static final NodeIdentifier FLOW_LABEL_NID = new NodeIdentifier(FlowLabel.QNAME);

    private FlowspecIpv6NlriParserHelper() {

    }

    public static void extractFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        if (fsType.findChildByArg(AbstractFlowspecNlriParser.DEST_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new DestinationIpv6PrefixCaseBuilder()
                .setDestinationPrefix(new Ipv6Prefix((String) fsType
                    .findChildByArg(AbstractFlowspecNlriParser.DEST_PREFIX_NID).get().body())).build());
        } else if (fsType.findChildByArg(AbstractFlowspecNlriParser.SOURCE_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new SourceIpv6PrefixCaseBuilder().setSourcePrefix(new Ipv6Prefix((String) fsType
                .findChildByArg(AbstractFlowspecNlriParser.SOURCE_PREFIX_NID).get().body())).build());
        } else if (fsType.findChildByArg(NEXT_HEADER_NID).isPresent()) {
            fsBuilder.setFlowspecType(new NextHeaderCaseBuilder()
                .setNextHeaders(createNextHeaders((UnkeyedListNode) fsType.findChildByArg(NEXT_HEADER_NID).get()))
                .build());
        } else if (fsType.findChildByArg(FLOW_LABEL_NID).isPresent()) {
            fsBuilder.setFlowspecType(new FlowLabelCaseBuilder()
                .setFlowLabel(createFlowLabels((UnkeyedListNode) fsType.findChildByArg(FLOW_LABEL_NID).get())).build());
        }
    }

    public static void buildFlowspecString(final FlowspecType value, final StringBuilder buffer) {
        if (value instanceof DestinationIpv6PrefixCase) {
            buffer.append("to ");
            buffer.append(((DestinationIpv6PrefixCase) value).getDestinationPrefix().getValue());
        } else if (value instanceof SourceIpv6PrefixCase) {
            buffer.append("from ");
            buffer.append(((SourceIpv6PrefixCase) value).getSourcePrefix().getValue());
        } else if (value instanceof NextHeaderCase) {
            buffer.append("where next header ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(((NextHeaderCase) value).getNextHeaders()));
        } else if (value instanceof FlowLabelCase) {
            buffer.append("where flow label ");
            buffer.append(stringFlowLabel(((FlowLabelCase) value).getFlowLabel()));
        }
    }

    private static List<NextHeaders> createNextHeaders(final UnkeyedListNode nextHeadersData) {
        final List<NextHeaders> nextHeaders = new ArrayList<>();

        for (final UnkeyedListEntryNode node : nextHeadersData.body()) {
            final NextHeadersBuilder nextHeadersBuilder = new NextHeadersBuilder();
            node.findChildByArg(AbstractFlowspecNlriParser.OP_NID).ifPresent(
                dataContainerChild -> nextHeadersBuilder.setOp(NumericOneByteOperandParser
                    .INSTANCE.create((Set<String>) dataContainerChild.body())));
            node.findChildByArg(AbstractFlowspecNlriParser.VALUE_NID).ifPresent(
                dataContainerChild -> nextHeadersBuilder.setValue((Uint8) dataContainerChild.body()));
            nextHeaders.add(nextHeadersBuilder.build());
        }

        return nextHeaders;
    }

    private static List<FlowLabel> createFlowLabels(final UnkeyedListNode flowLabelsData) {
        final List<FlowLabel> flowLabels = new ArrayList<>();

        for (final UnkeyedListEntryNode node : flowLabelsData.body()) {
            final FlowLabelBuilder flowLabelsBuilder = new FlowLabelBuilder();
            node.findChildByArg(AbstractFlowspecNlriParser.OP_NID).ifPresent(
                dataContainerChild -> flowLabelsBuilder.setOp(NumericOneByteOperandParser
                    .INSTANCE.create((Set<String>) dataContainerChild.body())));
            node.findChildByArg(AbstractFlowspecNlriParser.VALUE_NID).ifPresent(
                dataContainerChild -> flowLabelsBuilder.setValue((Uint32) dataContainerChild.body()));
            flowLabels.add(flowLabelsBuilder.build());
        }

        return flowLabels;
    }

    private static String stringFlowLabel(final List<FlowLabel> list) {
        final StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (final FlowLabel item : list) {
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }
}

