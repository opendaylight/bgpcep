/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.end.points;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.P2mpLeaves;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.P2mpIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.p2mp.ipv4._case.P2mpIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.p2mp.ipv4._case.P2mpIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPP2MPEndPointsIpv4ObjectParser extends CommonObjectParser {
    private static final int CLASS = 4;
    private static final int TYPE = 3;
    private static final int LEAF_TYPE_SIZE = 32;
    private static final Logger LOG = LoggerFactory.getLogger(PCEPP2MPEndPointsIpv4ObjectParser.class);

    public PCEPP2MPEndPointsIpv4ObjectParser() {
        super(CLASS, TYPE);
    }

    public static void serializeObject(
        final Boolean processing,
        final Boolean ignore,
        final P2mpIpv4 p2mpIpv4,
        final ByteBuf buffer) {
        final Set<Ipv4AddressNoZone> dest = p2mpIpv4.getDestinationIpv4Address();
        checkArgument(dest != null, "DestinationIpv4Address is mandatory.");
        final ByteBuf body = Unpooled.buffer(LEAF_TYPE_SIZE + Ipv4Util.IP4_LENGTH + Ipv4Util.IP4_LENGTH * dest.size());
        checkArgument(p2mpIpv4.getSourceIpv4Address() != null, "SourceIpv4Address is mandatory.");
        body.writeInt(p2mpIpv4.getP2mpLeaves().getIntValue());
        Ipv4Util.writeIpv4Address(p2mpIpv4.getSourceIpv4Address(), body);
        dest.forEach(ipv4 -> Ipv4Util.writeIpv4Address(ipv4, body));
        ObjectUtil.formatSubobject(TYPE, CLASS, processing, ignore, body, buffer);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        if (!header.getProcessingRule()) {
            LOG.debug("Processed bit not set on Endpoints OBJECT, ignoring it.");
            return new UnknownObject(PCEPErrors.P_FLAG_NOT_SET, builder.build());
        }
        if (bytes.readableBytes() % Ipv4Util.IP4_LENGTH != 0) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        builder.setIgnore(header.getIgnore());
        builder.setProcessingRule(header.getProcessingRule());
        final P2mpIpv4Builder p2mpIpv4Builder = new P2mpIpv4Builder();
        p2mpIpv4Builder.setP2mpLeaves(P2mpLeaves.forValue(bytes.readInt()));
        p2mpIpv4Builder.setSourceIpv4Address(Ipv4Util.addressForByteBuf(bytes));
        Set<Ipv4AddressNoZone> dest = new HashSet<>();
        while (bytes.isReadable()) {
            dest.add(Ipv4Util.addressForByteBuf(bytes));
        }
        p2mpIpv4Builder.setDestinationIpv4Address(dest);
        builder.setAddressFamily(new P2mpIpv4CaseBuilder().setP2mpIpv4(p2mpIpv4Builder.build()).build());
        return builder.build();
    }
}
