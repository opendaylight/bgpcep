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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for IPv4 {@link EndpointsObj}.
 */
public class PCEPEndPointsIpv4ObjectParser extends CommonObjectParser {

    private static final int CLASS = 4;
    private static final int TYPE = 1;
    private static final Logger LOG = LoggerFactory.getLogger(PCEPEndPointsIpv4ObjectParser.class);

    public PCEPEndPointsIpv4ObjectParser() {
        super(CLASS, TYPE);
    }

    public static void serializeObject(
        final Boolean processing,
        final Boolean ignore,
        final Ipv4 ipv4,
        final ByteBuf buffer) {
        final ByteBuf body = Unpooled.buffer(Ipv4Util.IP4_LENGTH + Ipv4Util.IP4_LENGTH);
        checkArgument(ipv4.getSourceIpv4Address() != null, "SourceIpv4Address is mandatory.");
        Ipv4Util.writeIpv4Address(ipv4.getSourceIpv4Address(), body);
        checkArgument(ipv4.getDestinationIpv4Address() != null, "DestinationIpv4Address is mandatory.");
        Ipv4Util.writeIpv4Address(ipv4.getDestinationIpv4Address(), body);
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
        if (bytes.readableBytes() != Ipv4Util.IP4_LENGTH * 2) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        final Ipv4Builder ipv4bldr = new Ipv4Builder()
                .setSourceIpv4Address(Ipv4Util.addressForByteBuf(bytes))
                .setDestinationIpv4Address(Ipv4Util.addressForByteBuf(bytes));
        builder.setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setAddressFamily(new Ipv4CaseBuilder().setIpv4(ipv4bldr.build()).build());
        return builder.build();
    }
}
