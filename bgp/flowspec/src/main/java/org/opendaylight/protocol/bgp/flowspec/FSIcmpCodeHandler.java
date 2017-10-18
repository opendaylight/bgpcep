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
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.IcmpCodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.icmp.code._case.Codes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.icmp.code._case.CodesBuilder;

public final class FSIcmpCodeHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int ICMP_CODE_VALUE = 8;

    @Override
    public void serializeType(FlowspecType fsType, ByteBuf output) {
        Preconditions.checkArgument(fsType instanceof IcmpCodeCase, "IcmpCodeCase class is mandatory!");
        output.writeByte(ICMP_CODE_VALUE);
        NumericOneByteOperandParser.INSTANCE.serialize(((IcmpCodeCase) fsType).getCodes(), output);
    }

    @Override
    public FlowspecType parseType(ByteBuf buffer) {
        requireNonNull(buffer, "input buffer is null, missing data to parse.");
        return new IcmpCodeCaseBuilder().setCodes(parseIcmpCode(buffer)).build();
    }

    private static List<Codes> parseIcmpCode(final ByteBuf nlri) {
        final List<Codes> codes = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final CodesBuilder builder = new CodesBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(nlri.readUnsignedByte());
            end = op.isEndOfList();
            codes.add(builder.build());
        }
        return codes;
    }
}
