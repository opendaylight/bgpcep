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
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.dscp._case.Dscps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.dscp._case.DscpsBuilder;

public final class FSDscpHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int DSCP_VALUE = 11;

    @Override
    public void serializeType(FlowspecType fsType, ByteBuf output) {
        Preconditions.checkArgument(fsType instanceof DscpCase, "DscpCase class is mandatory!");
        output.writeByte(DSCP_VALUE);
        serializeDscps(((DscpCase) fsType).getDscps(), output);
    }

    @Override
    public FlowspecType parseType(ByteBuf buffer) {
        if (buffer == null) {
            return null;
        }
        return new DscpCaseBuilder().setDscps(parseDscps(buffer)).build();
    }

    private static final void serializeDscps(final List<Dscps> dscps, final ByteBuf nlriByteBuf) {
        for (final Dscps dscp : dscps) {
            NumericOneByteOperandParser.INSTANCE.serialize(dscp.getOp(), 1, nlriByteBuf);
            Util.writeShortest(dscp.getValue().getValue(), nlriByteBuf);
        }
    }

    private static List<Dscps> parseDscps(final ByteBuf nlri) {
        final List<Dscps> dscps = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final DscpsBuilder builder = new DscpsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            // RFC does not specify operator
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Dscp(nlri.readUnsignedByte()));
            end = op.isEndOfList();
            dscps.add(builder.build());
        }
        return dscps;
    }
}
