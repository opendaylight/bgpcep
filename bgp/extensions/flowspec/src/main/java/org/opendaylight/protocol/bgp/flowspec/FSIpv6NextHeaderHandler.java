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
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.next.header._case.NextHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.next.header._case.NextHeadersBuilder;

public final class FSIpv6NextHeaderHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int NEXT_HEADER_VALUE = 3;

    @Override
    public void serializeType(FlowspecType value, ByteBuf output) {
        Preconditions.checkArgument(value instanceof NextHeaderCase, "NextHeaderCase class is mandatory!");
        output.writeByte(NEXT_HEADER_VALUE);
        NumericOneByteOperandParser.INSTANCE.serialize(((NextHeaderCase) value).getNextHeaders(), output);
    }

    @Override
    public FlowspecType parseType(ByteBuf buffer) {
        if (buffer == null) {
            return null;
        }
        return new NextHeaderCaseBuilder().setNextHeaders(parseNextHeader(buffer)).build();
    }

    private static List<NextHeaders> parseNextHeader(final ByteBuf nlri) {
        final List<NextHeaders> headers = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final NextHeadersBuilder builder = new NextHeadersBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(nlri.readUnsignedByte());
            end = op.isEndOfList();
            headers.add(builder.build());
        }
        return headers;
    }
}
