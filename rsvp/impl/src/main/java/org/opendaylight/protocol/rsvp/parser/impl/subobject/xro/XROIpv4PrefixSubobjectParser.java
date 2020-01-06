/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.subobject.xro;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;

public class XROIpv4PrefixSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {
    public static final int TYPE = 1;

    private static final int PREFIX_F_LENGTH = 1;
    private static final int PREFIX4_F_OFFSET = Ipv4Util.IP4_LENGTH;
    private static final int CONTENT4_LENGTH = PREFIX4_F_OFFSET + PREFIX_F_LENGTH + 1;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean mandatory)
            throws RSVPParsingException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        builder.setMandatory(mandatory);
        if (buffer.readableBytes() != CONTENT4_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final int length = buffer.getUnsignedByte(PREFIX4_F_OFFSET);
        final IpPrefixBuilder prefix = new IpPrefixBuilder().setIpPrefix(new IpPrefix(
            Ipv4Util.prefixForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH), length)));
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(prefix.build()).build());
        buffer.skipBytes(PREFIX_F_LENGTH);
        builder.setAttribute(ExcludeRouteSubobjects.Attribute.forValue(buffer.readUnsignedByte()));
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        final SubobjectType type = subobject.getSubobjectType();
        checkArgument(type instanceof IpPrefixCase, "Unknown subobject instance. Passed %s. Needed IpPrefixCase.",
            type.getClass());
        final IpPrefixSubobject specObj = ((IpPrefixCase) type).getIpPrefix();
        final IpPrefix prefix = specObj.getIpPrefix();
        final Ipv6Prefix ipv6Prefix = prefix.getIpv6Prefix();
        if (ipv6Prefix != null) {
            XROIpv6PrefixSubobjectParser.serializeSubobject(buffer, subobject, ipv6Prefix);
            return;
        }

        final ByteBuf body = Unpooled.buffer(CONTENT4_LENGTH);
        checkArgument(prefix.getIpv4Prefix() != null, "Ipv4Prefix is mandatory.");
        Ipv4Util.writeIpv4Prefix(prefix.getIpv4Prefix(), body);
        final Attribute attribute = subobject.getAttribute();
        checkArgument(attribute != null, "Attribute is mandatory.");
        body.writeByte(attribute.getIntValue());
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
