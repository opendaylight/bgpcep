/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FlowLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.NextHeaderCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.next.header._case.NextHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.next.header._case.NextHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public final class SimpleFlowspecIpv6NlriParser extends AbstractFlowspecNlriParser {

    static final NodeIdentifier NEXT_HEADER_NID = new NodeIdentifier(NextHeaders.QNAME);
    static final NodeIdentifier FLOW_LABEL_NID = new NodeIdentifier(FlowLabel.QNAME);

    public SimpleFlowspecIpv6NlriParser(SimpleFlowspecTypeRegistry flowspecTypeRegistry) {
        this.flowspecTypeRegistry = flowspecTypeRegistry;
    }

    @Override
    DestinationType createWithdrawnDestinationType(final List<Flowspec> dst) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6CaseBuilder().setDestinationFlowspec(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder().setFlowspec(
                dst).build()).build();
    }

    @Override
    DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
            .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder()
                .setFlowspec(dst).build()).build();
    }

    @Override
    protected void serializeMpReachNlri(final Attributes1 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final AdvertizedRoutes routes = (pathAttributes.getMpReachNlri()).getAdvertizedRoutes();
        if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) routes.getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }

    @Override
    public void extractSpecificFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        if (fsType.getChild(DEST_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new DestinationIpv6PrefixCaseBuilder()
                .setDestinationPrefix(new Ipv6Prefix((String) fsType.getChild(DEST_PREFIX_NID).get().getValue()))
                .build());
        } else if (fsType.getChild(SOURCE_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new SourceIpv6PrefixCaseBuilder()
                .setSourcePrefix(new Ipv6Prefix((String) fsType.getChild(SOURCE_PREFIX_NID).get().getValue()))
                .build());
        } else if (fsType.getChild(NEXT_HEADER_NID).isPresent()) {
            fsBuilder.setFlowspecType(new NextHeaderCaseBuilder().setNextHeaders(createNextHeaders((UnkeyedListNode) fsType.getChild(NEXT_HEADER_NID).get())).build());
        } else if (fsType.getChild(FLOW_LABEL_NID).isPresent()) {
            fsBuilder.setFlowspecType(new FlowLabelCaseBuilder().setFlowLabel(createFlowLabels((UnkeyedListNode) fsType.getChild(FLOW_LABEL_NID).get())).build());
        }
    }

    private List<NextHeaders> createNextHeaders(final UnkeyedListNode nextHeadersData) {
        final List<NextHeaders> nextHeaders = new ArrayList<>();

        for (final UnkeyedListEntryNode node : nextHeadersData.getValue()) {
            final NextHeadersBuilder nextHeadersBuilder = new NextHeadersBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                nextHeadersBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                nextHeadersBuilder.setValue((Short) valueNode.get().getValue());
            }
            nextHeaders.add(nextHeadersBuilder.build());
        }

        return nextHeaders;
    }

    private List<FlowLabel> createFlowLabels(final UnkeyedListNode flowLabelsData) {
        final List<FlowLabel> flowLabels = new ArrayList<>();

        for (final UnkeyedListEntryNode node : flowLabelsData.getValue()) {
            final FlowLabelBuilder flowLabelsBuilder = new FlowLabelBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                flowLabelsBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                flowLabelsBuilder.setValue((Long) valueNode.get().getValue());
            }
            flowLabels.add(flowLabelsBuilder.build());
        }

        return flowLabels;
    }

    @Override
    protected void stringSpecificFSNlriType(final FlowspecType value, final StringBuilder buffer) {
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

    @Override
    protected void serializeMpUnreachNlri(final Attributes2 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final MpUnreachNlri mpUnreachNlri = pathAttributes.getMpUnreachNlri();
        if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationFlowspecIpv6Case) {
            final DestinationFlowspecIpv6Case flowspecCase = (DestinationFlowspecIpv6Case) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }
}
