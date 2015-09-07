/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for IPv4 {@link EndpointsObj}
 */
public class PCEPEndPointsIpv4ObjectParser implements ObjectParser, ObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPEndPointsIpv4ObjectParser.class);

    public static final int CLASS = 4;

    public static final int TYPE = 1;

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        if (!header.isProcessingRule()) {
            LOG.debug("Processed bit not set on Endpoints OBJECT, ignoring it.");
            return new UnknownObject(PCEPErrors.P_FLAG_NOT_SET, builder.build());
        }
        if (bytes.readableBytes() != Ipv4Util.IP4_LENGTH * 2) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final Ipv4Builder b = new Ipv4Builder();
        b.setSourceIpv4Address(Ipv4Util.addressForByteBuf(bytes));
        b.setDestinationIpv4Address((Ipv4Util.addressForByteBuf(bytes)));
        builder.setAddressFamily(new Ipv4CaseBuilder().setIpv4(b.build()).build());
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof EndpointsObj, "Wrong instance of PCEPObject. Passed %s. Needed EndpointsObject.", object.getClass());
        final EndpointsObj ePObj = (EndpointsObj) object;
        final AddressFamily afi = ePObj.getAddressFamily();
        if(afi instanceof Ipv6Case) {
            PCEPEndPointsIpv6ObjectParser.serializeObject(object,buffer);
        }
        Preconditions.checkArgument(afi instanceof Ipv4Case, "Wrong instance of NetworkAddress. Passed %s. Needed IPv4", afi.getClass());
        final Ipv4 ipv4 = ((Ipv4Case) afi).getIpv4();
        final ByteBuf body = Unpooled.buffer(Ipv4Util.IP4_LENGTH + Ipv4Util.IP4_LENGTH);
        Preconditions.checkArgument(ipv4.getSourceIpv4Address() != null, "SourceIpv4Address is mandatory.");
        writeIpv4Address(ipv4.getSourceIpv4Address(), body);
        Preconditions.checkArgument(ipv4.getDestinationIpv4Address() != null, "DestinationIpv4Address is mandatory.");
        writeIpv4Address(ipv4.getDestinationIpv4Address(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
