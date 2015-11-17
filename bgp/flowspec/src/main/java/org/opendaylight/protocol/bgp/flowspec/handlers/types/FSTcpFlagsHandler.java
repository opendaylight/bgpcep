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
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.TcpFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlagsBuilder;

public final class FSTcpFlagsHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int TCP_FLAGS_VALUE = 9;

    @Override
    public void serializeType(FlowspecType fsType, ByteBuf output) {
        Preconditions.checkArgument(fsType instanceof TcpFlagsCase, "TcpFlagsCase class is mandatory!");
        output.writeByte(TCP_FLAGS_VALUE);
        serializeTcpFlags(((TcpFlagsCase) fsType).getTcpFlags(), output);
    }

    @Override
    public FlowspecType parseType(ByteBuf buffer) {
        if (buffer == null) {
            return null;
        }
        return new TcpFlagsCaseBuilder().setTcpFlags(parseTcpFlags(buffer)).build();
    }

    private static final void serializeTcpFlags(final List<TcpFlags> flags, final ByteBuf nlriByteBuf) {
        for (final TcpFlags flag : flags) {
            final ByteBuf flagsBuf = Unpooled.buffer();
            Util.writeShortest(flag.getValue(), flagsBuf);
            BitmaskOperandParser.INSTANCE.serialize(flag.getOp(), flagsBuf.readableBytes(), nlriByteBuf);
            nlriByteBuf.writeBytes(flagsBuf);
        }
    }

    private static List<TcpFlags> parseTcpFlags(final ByteBuf nlri) {
        final List<TcpFlags> flags = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final TcpFlagsBuilder builder = new TcpFlagsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final BitmaskOperand op = BitmaskOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            flags.add(builder.build());
        }
        return flags;
    }
}
