/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Prefix;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

/**
 * Parser for {@link IpPrefixCase}
 */
public class XROIpv4PrefixSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

    public static final int TYPE = 1;

    private static final int PREFIX_F_LENGTH = 1;

    private static final int PREFIX4_F_OFFSET = Ipv4Util.IP4_LENGTH;

    private static final int CONTENT4_LENGTH = PREFIX4_F_OFFSET + PREFIX_F_LENGTH + 1;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setMandatory(mandatory);
        if (buffer.readableBytes() != CONTENT4_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final int length = buffer.getUnsignedByte(PREFIX4_F_OFFSET);
        final IpPrefixBuilder prefix = new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(buffer,
                Ipv4Util.IP4_LENGTH), length)));
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(prefix.build()).build());
        buffer.skipBytes(PREFIX_F_LENGTH);
        builder.setAttribute(Attribute.forValue(buffer.readUnsignedByte()));
        return builder.build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof IpPrefixCase, "Unknown subobject instance. Passed %s. Needed IpPrefixCase.", subobject.getSubobjectType().getClass());
        final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
        final IpPrefix prefix = specObj.getIpPrefix();
        Preconditions.checkArgument(prefix.getIpv4Prefix() != null || prefix.getIpv6Prefix() != null, "Unknown AbstractPrefix instance. Passed %s.", prefix.getClass());
        if (prefix.getIpv6Prefix() != null) {
            new XROIpv6PrefixSubobjectParser().serializeSubobject(subobject, buffer);
        } else {
            final ByteBuf body = Unpooled.buffer(CONTENT4_LENGTH);
            Preconditions.checkArgument(prefix.getIpv4Prefix() != null, "Ipv4Prefix is mandatory.");
            writeIpv4Prefix(prefix.getIpv4Prefix(), body);
            Preconditions.checkArgument(subobject.getAttribute() != null, "Attribute is mandatory.");
            writeUnsignedByte((short) subobject.getAttribute().getIntValue(), body);
            XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
        }
    }
}
