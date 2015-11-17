/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;

public final class FSDestinationPrefixHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    static final int DESTINATION_PREFIX_VALUE = 1;

    @Override
    public void serializeType(FlowspecType value, ByteBuf output) {
        Preconditions.checkArgument(value instanceof DestinationPrefixCase, "DestinationPrefixCase class is mandatory!");
        output.writeByte(DESTINATION_PREFIX_VALUE);
        output.writeBytes(Ipv4Util.bytesForPrefixBegin(((DestinationPrefixCase) value).getDestinationPrefix()));
    }

    @Override
    public FlowspecType parseType(ByteBuf buffer) {
        Preconditions.checkArgument(((int) buffer.readUnsignedByte()) == DESTINATION_PREFIX_VALUE, "Destination prefix type does not match!");
        return new DestinationPrefixCaseBuilder().setDestinationPrefix(Ipv4Util.prefixForByteBuf(buffer)).build();
    }
}
