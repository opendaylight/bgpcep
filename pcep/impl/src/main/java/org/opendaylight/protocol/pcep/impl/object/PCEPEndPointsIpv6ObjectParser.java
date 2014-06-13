/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for IPv6 {@link EndpointsObj}
 */
public class PCEPEndPointsIpv6ObjectParser extends AbstractObjectWithTlvsParser<EndpointsObjBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPEndPointsIpv6ObjectParser.class);

    public static final int CLASS = 4;

    public static final int TYPE = 2;

    public PCEPEndPointsIpv6ObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        if (!header.isProcessingRule()) {
            LOG.debug("Processed bit not set on Endpoints OBJECT, ignoring it.");
            return new UnknownObject(PCEPErrors.P_FLAG_NOT_SET, builder.build());
        }
        if (bytes.readableBytes() != Ipv6Util.IPV6_LENGTH * 2) {
            throw new PCEPDeserializerException("Wrong length of array of bytes.");
        }
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final Ipv6Builder b = new Ipv6Builder();
        b.setSourceIpv6Address(Ipv6Util.addressForBytes(ByteArray.readBytes(bytes, Ipv6Util.IPV6_LENGTH)));
        b.setDestinationIpv6Address((Ipv6Util.addressForBytes(ByteArray.readBytes(bytes, Ipv6Util.IPV6_LENGTH))));
        builder.setAddressFamily(new Ipv6CaseBuilder().setIpv6(b.build()).build());
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof EndpointsObj, "Wrong instance of PCEPObject. Passed %s. Needed EndpointsObject.", object.getClass());
        final EndpointsObj ePObj = (EndpointsObj) object;
        final AddressFamily afi = ePObj.getAddressFamily();
        Preconditions.checkArgument(afi instanceof Ipv6Case, "Wrong instance of NetworkAddress. Passed %s. Needed IPv6", afi.getClass());
        final ByteBuf body = Unpooled.buffer(Ipv6Util.IPV6_LENGTH + Ipv6Util.IPV6_LENGTH);
        body.writeBytes(Ipv6Util.bytesForAddress((((Ipv6Case) afi).getIpv6()).getSourceIpv6Address()));
        body.writeBytes(Ipv6Util.bytesForAddress((((Ipv6Case) afi).getIpv6()).getDestinationIpv6Address()));
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
