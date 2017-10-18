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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;

public final class FSIpv6DestinationPrefixHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int IPV6_DESTINATION_PREFIX_VALUE = 1;

    @Override
    public void serializeType(final FlowspecType value, final ByteBuf output) {
        Preconditions.checkArgument(value instanceof DestinationIpv6PrefixCase, "DestinationIpv6PrefixCase class is mandatory!");
        output.writeByte(IPV6_DESTINATION_PREFIX_VALUE);

        FSIpv6SourcePrefixHandler.writePrefix(((DestinationIpv6PrefixCase) value).getDestinationPrefix(), output);
    }

    @Override
    public FlowspecType parseType(final ByteBuf buffer) {
        requireNonNull(buffer, "input buffer is null, missing data to parse.");
        return new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(
                FSIpv6SourcePrefixHandler.parseIpv6Prefix(buffer)).build();
    }
}
