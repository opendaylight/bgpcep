/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

/**
 * Parser for {@link IpPrefixCase}.
 */
public class XROIpv4PrefixSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {
    public static final int TYPE = 1;

    private static final int PREFIX_F_LENGTH = 1;
    private static final int PREFIX4_F_OFFSET = Ipv4Util.IP4_LENGTH;
    private static final int CONTENT4_LENGTH = PREFIX4_F_OFFSET + PREFIX_F_LENGTH + 1;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT4_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + ";");
        }
        final int length = buffer.getUnsignedByte(PREFIX4_F_OFFSET);
        final IpPrefixBuilder prefix = new IpPrefixBuilder()
                .setIpPrefix(new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH),
                    length)));
        buffer.skipBytes(PREFIX_F_LENGTH);
        return new SubobjectBuilder()
                .setMandatory(mandatory)
                .setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(prefix.build()).build())
                .setAttribute(Attribute.forValue(buffer.readUnsignedByte()))
                .build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof IpPrefixCase,
            "Unknown subobject instance. Passed %s. Needed IpPrefixCase.", subobject.getSubobjectType().getClass());
        final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
        final IpPrefix prefix = specObj.getIpPrefix();
        final Ipv6Prefix ipv6Prefix = prefix.getIpv6Prefix();
        if (ipv6Prefix != null) {
            XROIpv6PrefixSubobjectParser.serializeSubobject(buffer, subobject, ipv6Prefix);
            return;
        }

        final Ipv4Prefix ipv4Prefix = prefix.getIpv4Prefix();
        checkArgument(ipv4Prefix != null, "Ipv4Prefix is mandatory.");

        final ByteBuf body = Unpooled.buffer(CONTENT4_LENGTH);
        Ipv4Util.writeIpv4Prefix(ipv4Prefix, body);
        final Attribute attribute = subobject.getAttribute();
        checkArgument(attribute != null, "Attribute is mandatory.");
        body.writeByte(attribute.getIntValue());
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
