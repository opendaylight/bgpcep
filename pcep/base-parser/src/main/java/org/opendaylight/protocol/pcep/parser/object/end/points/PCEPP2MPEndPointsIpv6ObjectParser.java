/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object.end.points;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Address;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.P2mpLeaves;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.P2mpIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.p2mp.ipv6._case.P2mpIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.p2mp.ipv6._case.P2mpIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPP2MPEndPointsIpv6ObjectParser extends CommonObjectParser {
    private static final int CLASS = 4;
    private static final int TYPE = 4;
    private static final int LEAF_TYPE_SIZE = 32;
    private static final Logger LOG = LoggerFactory.getLogger(PCEPP2MPEndPointsIpv6ObjectParser.class);

    public PCEPP2MPEndPointsIpv6ObjectParser() {
        super(CLASS, TYPE);
    }

    public static void serializeObject(
        final Boolean processing,
        final Boolean ignore,
        final P2mpIpv6 p2mpIpv6,
        final ByteBuf buffer) {
        final List<Ipv6AddressNoZone> dest = p2mpIpv6.getDestinationIpv6Address();
        Preconditions.checkArgument(dest != null, "DestinationIpv6Address is mandatory.");
        final ByteBuf body = Unpooled.buffer(LEAF_TYPE_SIZE + Ipv6Util.IPV6_LENGTH + Ipv6Util.IPV6_LENGTH * dest.size());
        Preconditions.checkArgument(p2mpIpv6.getSourceIpv6Address() != null,
            "SourceIpv6Address is mandatory.");
        body.writeInt(p2mpIpv6.getP2mpLeaves().getIntValue());
        writeIpv6Address(p2mpIpv6.getSourceIpv6Address(), body);
        dest.forEach(ipv6 -> writeIpv6Address(ipv6, body));
        ObjectUtil.formatSubobject(TYPE, CLASS, processing, ignore, body, buffer);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        if (!header.isProcessingRule()) {
            LOG.debug("Processed bit not set on Endpoints OBJECT, ignoring it.");
            return new UnknownObject(PCEPErrors.P_FLAG_NOT_SET, builder.build());
        }
        if (bytes.readableBytes() % Ipv6Util.IPV6_LENGTH != 4) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final P2mpIpv6Builder p2mpIpv6Builder = new P2mpIpv6Builder();
        p2mpIpv6Builder.setP2mpLeaves(P2mpLeaves.forValue(bytes.readInt()));
        p2mpIpv6Builder.setSourceIpv6Address(Ipv6Util.noZoneAddressForByteBuf(bytes));
        List<Ipv6AddressNoZone> dest = new ArrayList<>();
        while (bytes.isReadable()) {
            dest.add(Ipv6Util.noZoneAddressForByteBuf(bytes));
        }
        p2mpIpv6Builder.setDestinationIpv6Address(dest);
        builder.setAddressFamily(new P2mpIpv6CaseBuilder().setP2mpIpv6(p2mpIpv6Builder.build()).build());
        return builder.build();
    }
}
