/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCaseBuilder;

public final class FSIpv4SourcePrefixHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int SOURCE_PREFIX_VALUE = 2;

    @Override
    public void serializeType(final FlowspecType value, final ByteBuf output) {
        Preconditions.checkArgument(value instanceof SourcePrefixCase, "SourcePrefixCase class is mandatory!");
        output.writeByte(SOURCE_PREFIX_VALUE);
        ByteBufWriteUtil.writeMinimalPrefix(((SourcePrefixCase) value).getSourcePrefix(), output);
    }

    @Override
    public FlowspecType parseType(final ByteBuf buffer) {
        requireNonNull(buffer, "input buffer is null, missing data to parse.");
        return new SourcePrefixCaseBuilder().setSourcePrefix(Ipv4Util.prefixForByteBuf(buffer)).build();
    }
}
