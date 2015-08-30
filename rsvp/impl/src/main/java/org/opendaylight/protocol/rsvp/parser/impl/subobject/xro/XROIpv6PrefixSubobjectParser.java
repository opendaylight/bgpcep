/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.xro;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Prefix;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;

/**
 * Parser for {@link IpPrefixCase}
 */
public class XROIpv6PrefixSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

    public static final int TYPE = 2;

    private static final int PREFIX_F_LENGTH = 1;

    private static final int PREFIX6_F_OFFSET = Ipv6Util.IPV6_LENGTH;

    private static final int CONTENT6_LENGTH = PREFIX6_F_OFFSET + PREFIX_F_LENGTH + 1;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean mandatory) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        builder.setMandatory(mandatory);
        if (buffer.readableBytes() != CONTENT6_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final int length = buffer.getUnsignedByte(PREFIX6_F_OFFSET);
        final IpPrefixBuilder prefix = new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(buffer,
            Ipv6Util.IPV6_LENGTH), length)));
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(prefix.build()).build());
        buffer.skipBytes(PREFIX_F_LENGTH);
        builder.setAttribute(ExcludeRouteSubobjects.Attribute.forValue(buffer.readUnsignedByte()));
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof IpPrefixCase, "Unknown subobject instance. Passed %s. Needed IpPrefixCase.", subobject.getSubobjectType().getClass());
        final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
        final IpPrefix prefix = specObj.getIpPrefix();
        final ByteBuf body = Unpooled.buffer(CONTENT6_LENGTH);
        Preconditions.checkArgument(prefix.getIpv6Prefix() != null, "Ipv6Prefix is mandatory.");
        writeIpv6Prefix(prefix.getIpv6Prefix(), body);
        Preconditions.checkArgument(subobject.getAttribute() != null, "Attribute is mandatory.");
        writeUnsignedByte((short) subobject.getAttribute().getIntValue(), body);
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
