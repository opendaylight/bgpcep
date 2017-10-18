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
import java.util.Iterator;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.bgp.flowspec.handlers.NumericOneByteOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.DscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.dscp._case.Dscps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.dscp._case.DscpsBuilder;

public final class FSDscpHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    static final int DSCP_VALUE = 11;

    private static void serializeDscps(final List<Dscps> dscps, final ByteBuf nlriByteBuf) {
        for (final Iterator<Dscps> it = dscps.iterator(); it.hasNext(); ) {
            final Dscps dscp = it.next();
            NumericOneByteOperandParser.INSTANCE.serialize(dscp.getOp(), 1, !it.hasNext(), nlriByteBuf);
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
            builder.setValue(new Dscp(nlri.readUnsignedByte()));
            end = op.isEndOfList();
            dscps.add(builder.build());
        }
        return dscps;
    }

    @Override
    public void serializeType(final FlowspecType fsType, final ByteBuf output) {
        Preconditions.checkArgument(fsType instanceof DscpCase, "DscpCase class is mandatory!");
        output.writeByte(DSCP_VALUE);
        serializeDscps(((DscpCase) fsType).getDscps(), output);
    }

    @Override
    public FlowspecType parseType(final ByteBuf buffer) {
        requireNonNull(buffer, "input buffer is null, missing data to parse.");
        return new DscpCaseBuilder().setDscps(parseDscps(buffer)).build();
    }
}
