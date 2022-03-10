/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.unreach;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.UnreachDestinationObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.UnreachDestinationObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.unreach.destination.obj.destination.Ipv6DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.unreach.destination.obj.destination.Ipv6DestinationCaseBuilder;

public final class PCEPIpv6UnreachDestinationParser extends CommonObjectParser {
    private static final int CLASS = 28;
    private static final int TYPE = 2;

    public PCEPIpv6UnreachDestinationParser() {
        super(CLASS, TYPE);
    }

    public static void serializeObject(
        final Boolean processing,
        final Boolean ignore,
        final Ipv6DestinationCase ipv6Case,
        final ByteBuf buffer) {
        final Set<Ipv6AddressNoZone> dest = ipv6Case.getDestinationIpv6Address();
        checkArgument(dest != null, "Destinationipv6Address is mandatory.");
        final ByteBuf body = Unpooled.buffer(Ipv6Util.IPV6_LENGTH * dest.size());
        dest.forEach(ipv6 -> Ipv6Util.writeIpv6Address(ipv6, body));
        ObjectUtil.formatSubobject(TYPE, CLASS, processing, ignore, body, buffer);
    }

    @Override
    public UnreachDestinationObj parseObject(final ObjectHeader header, final ByteBuf bytes)
        throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final UnreachDestinationObjBuilder builder = new UnreachDestinationObjBuilder();
        if (bytes.readableBytes() % Ipv6Util.IPV6_LENGTH != 0) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        builder.setIgnore(header.getIgnore());
        builder.setProcessingRule(header.getProcessingRule());
        Set<Ipv6AddressNoZone> dest = new HashSet<>();
        while (bytes.isReadable()) {
            dest.add(Ipv6Util.addressForByteBuf(bytes));
        }
        return builder.setDestination(new Ipv6DestinationCaseBuilder().setDestinationIpv6Address(dest).build())
                .build();
    }
}
