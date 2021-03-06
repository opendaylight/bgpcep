/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.ipv4;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

/**
 * Helper for parsing IPv4 Flowspec.
 *
 * @author Kevin Wang
 */
public final class FlowspecIpv4NlriParserHelper {
    private static final NodeIdentifier PROTOCOL_IP_NID = new NodeIdentifier(ProtocolIps.QNAME);

    private FlowspecIpv4NlriParserHelper() {

    }

    public static void extractFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        if (fsType.findChildByArg(AbstractFlowspecNlriParser.DEST_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(
                    new Ipv4Prefix((String) fsType.findChildByArg(AbstractFlowspecNlriParser.DEST_PREFIX_NID).get()
                            .body())).build());
        } else if (fsType.findChildByArg(AbstractFlowspecNlriParser.SOURCE_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix((String) fsType
                    .findChildByArg(AbstractFlowspecNlriParser.SOURCE_PREFIX_NID).get().body())).build());
        } else if (fsType.findChildByArg(PROTOCOL_IP_NID).isPresent()) {
            fsBuilder.setFlowspecType(new ProtocolIpCaseBuilder()
                    .setProtocolIps(createProtocolsIps((UnkeyedListNode) fsType.findChildByArg(PROTOCOL_IP_NID).get()))
                    .build());
        }
    }

    public static void buildFlowspecString(final FlowspecType value, final StringBuilder buffer) {
        if (value instanceof DestinationPrefixCase) {
            buffer.append("to ");
            buffer.append(((DestinationPrefixCase) value).getDestinationPrefix().getValue());
        } else if (value instanceof SourcePrefixCase) {
            buffer.append("from ");
            buffer.append(((SourcePrefixCase) value).getSourcePrefix().getValue());
        } else if (value instanceof ProtocolIpCase) {
            buffer.append("where IP protocol ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(((ProtocolIpCase) value).getProtocolIps()));
        }
    }

    private static List<ProtocolIps> createProtocolsIps(final UnkeyedListNode protocolIpsData) {
        final List<ProtocolIps> protocolIps = new ArrayList<>();

        for (final UnkeyedListEntryNode node : protocolIpsData.body()) {
            final ProtocolIpsBuilder ipsBuilder = new ProtocolIpsBuilder();
            node.findChildByArg(AbstractFlowspecNlriParser.OP_NID).ifPresent(
                dataContainerChild -> ipsBuilder.setOp(NumericOneByteOperandParser
                    .INSTANCE.create((Set<String>) dataContainerChild.body())));
            node.findChildByArg(AbstractFlowspecNlriParser.VALUE_NID).ifPresent(
                dataContainerChild -> ipsBuilder.setValue((Uint8) dataContainerChild.body()));
            protocolIps.add(ipsBuilder.build());
        }

        return protocolIps;
    }
}

