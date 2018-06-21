/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.handlers.AbstractOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabelBuilder;

public final class FSIpv6FlowLabelHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    static final int FLOW_LABEL_VALUE = 13;

    private static void serializeNumericFourByteValue(final List<FlowLabel> list, final ByteBuf nlriByteBuf) {
        for (final Iterator<FlowLabel> it = list.iterator(); it.hasNext(); ) {
            final FlowLabel label = it.next();
            final ByteBuf protoBuf = Unpooled.buffer();
            Util.writeShortest(label.getValue().intValue(), protoBuf);
            NumericOneByteOperandParser.INSTANCE.serialize(label.getOp(), protoBuf.readableBytes(),
                    !it.hasNext(), nlriByteBuf);
            nlriByteBuf.writeBytes(protoBuf);
        }
    }

    @Override
    public void serializeType(final FlowspecType fsType, final ByteBuf output) {
        Preconditions.checkArgument(fsType instanceof FlowLabelCase, "FlowLabelCase class is mandatory!");
        output.writeByte(FLOW_LABEL_VALUE);
        serializeNumericFourByteValue(((FlowLabelCase) fsType).getFlowLabel(), output);
    }

    @Override
    public FlowspecType parseType(final ByteBuf buffer) {
        return new FlowLabelCaseBuilder().setFlowLabel(parseFlowLabel(buffer)).build();
    }

    private static List<FlowLabel> parseFlowLabel(final ByteBuf nlri) {
        final List<FlowLabel> labels = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final FlowLabelBuilder builder = new FlowLabelBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
            builder.setValue(ByteArray.bytesToLong(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            labels.add(builder.build());
        }
        return labels;
    }
}
