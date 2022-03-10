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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.UnreachDestinationObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.UnreachDestinationObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.unreach.destination.obj.destination.Ipv4DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.unreach.destination.obj.destination.Ipv4DestinationCaseBuilder;

public final class PCEPIpv4UnreachDestinationParser extends CommonObjectParser {
    private static final int CLASS = 28;
    private static final int TYPE = 1;

    public PCEPIpv4UnreachDestinationParser() {
        super(CLASS, TYPE);
    }

    public static void serializeObject(
        final Boolean processing,
        final Boolean ignore,
        final Ipv4DestinationCase ipv4Case,
        final ByteBuf buffer) {
        final Set<Ipv4AddressNoZone> dest = ipv4Case.getDestinationIpv4Address();
        checkArgument(dest != null, "DestinationIpv4Address is mandatory.");
        final ByteBuf body = Unpooled.buffer(Ipv4Util.IP4_LENGTH * dest.size());
        dest.forEach(ipv4 -> Ipv4Util.writeIpv4Address(ipv4, body));
        ObjectUtil.formatSubobject(TYPE, CLASS, processing, ignore, body, buffer);
    }

    @Override
    public UnreachDestinationObj parseObject(final ObjectHeader header, final ByteBuf bytes)
        throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final UnreachDestinationObjBuilder builder = new UnreachDestinationObjBuilder();
        if (bytes.readableBytes() % Ipv4Util.IP4_LENGTH != 0) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        builder.setIgnore(header.getIgnore());
        builder.setProcessingRule(header.getProcessingRule());
        Set<Ipv4AddressNoZone> dest = new HashSet<>();
        while (bytes.isReadable()) {
            dest.add(Ipv4Util.addressForByteBuf(bytes));
        }
        builder.setDestination(new Ipv4DestinationCaseBuilder().setDestinationIpv4Address(dest).build());
        return builder.build();
    }
}
