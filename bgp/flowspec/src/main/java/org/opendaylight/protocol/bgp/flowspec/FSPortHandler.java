/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.handlers.AbstractOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericTwoByteOperandParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.port._case.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.port._case.PortsBuilder;

public final class FSPortHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int PORT_VALUE = 4;

    @Override
    public void serializeType(FlowspecType fsType, ByteBuf output) {
        Preconditions.checkArgument(fsType instanceof PortCase, "PortCase class is mandatory!");
        output.writeByte(PORT_VALUE);
        NumericTwoByteOperandParser.INSTANCE.serialize(((PortCase) fsType).getPorts(), output);
    }

    @Override
    public FlowspecType parseType(ByteBuf buffer) {
        requireNonNull(buffer, "input buffer is null, missing data to parse.");
        return new PortCaseBuilder().setPorts(parsePort(buffer)).build();
    }

    private static List<Ports> parsePort(final ByteBuf nlri) {
        final List<Ports> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final PortsBuilder builder = new PortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ports.add(builder.build());
        }
        return ports;
    }
}
