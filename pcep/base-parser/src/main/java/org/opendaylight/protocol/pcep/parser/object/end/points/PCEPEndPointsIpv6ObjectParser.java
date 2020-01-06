/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.end.points;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for IPv6 {@link EndpointsObj}.
 */
public final class PCEPEndPointsIpv6ObjectParser extends CommonObjectParser {

    private static final int CLASS = 4;
    private static final int TYPE = 2;
    private static final Logger LOG = LoggerFactory.getLogger(PCEPEndPointsIpv6ObjectParser.class);

    public PCEPEndPointsIpv6ObjectParser() {
        super(CLASS, TYPE);
    }

    public static void serializeObject(
        final Boolean processing,
        final Boolean ignore,
        final Ipv6 ipv6,
        final ByteBuf buffer) {
        final ByteBuf body = Unpooled.buffer(Ipv6Util.IPV6_LENGTH + Ipv6Util.IPV6_LENGTH);
        checkArgument(ipv6.getSourceIpv6Address() != null, "SourceIpv6Address is mandatory.");
        Ipv6Util.writeIpv6Address(ipv6.getSourceIpv6Address(), body);
        checkArgument(ipv6.getDestinationIpv6Address() != null, "DestinationIpv6Address is mandatory.");
        Ipv6Util.writeIpv6Address(ipv6.getDestinationIpv6Address(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, processing, ignore, body, buffer);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        if (!header.isProcessingRule()) {
            LOG.debug("Processed bit not set on Endpoints OBJECT, ignoring it.");
            return new UnknownObject(PCEPErrors.P_FLAG_NOT_SET, builder.build());
        }
        if (bytes.readableBytes() != Ipv6Util.IPV6_LENGTH * 2) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        final Ipv6Builder ipv6bldr = new Ipv6Builder()
                .setSourceIpv6Address(Ipv6Util.noZoneAddressForByteBuf(bytes))
                .setDestinationIpv6Address(Ipv6Util.noZoneAddressForByteBuf(bytes));
        builder.setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setAddressFamily(new Ipv6CaseBuilder().setIpv6(ipv6bldr.build()).build());
        return builder.build();
    }
}
