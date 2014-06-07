/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.impl.object.XROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

/**
 * Parser for {@link IpPrefixCase}
 */
public class XROIpv6PrefixSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

    public static final int TYPE = 2;

    private static final int PREFIX_F_LENGTH = 1;
    private static final int ATTRIBUTE_LENGTH = 1;

    private static final int IP_F_OFFSET = 0;

    private static final int IP6_F_LENGTH = 16;
    private static final int PREFIX6_F_OFFSET = IP_F_OFFSET + IP6_F_LENGTH;
    private static final int ATTRIBUTE6_OFFSET = PREFIX6_F_OFFSET + PREFIX_F_LENGTH;

    private static final int CONTENT6_LENGTH = ATTRIBUTE6_OFFSET + ATTRIBUTE_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setMandatory(mandatory);
        if (buffer.readableBytes() != CONTENT6_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final int length = UnsignedBytes.toInt(buffer.getByte(PREFIX6_F_OFFSET));
        IpPrefixBuilder prefix = new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(buffer,
                IP6_F_LENGTH), length)));
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(prefix.build()).build());
        buffer.readerIndex(buffer.readerIndex() + PREFIX_F_LENGTH);
        builder.setAttribute(Attribute.forValue(UnsignedBytes.toInt(buffer.readByte())));
        return builder.build();
    }

    @Override
    public byte[] serializeSubobject(final Subobject subobject) {
        if (!(subobject.getSubobjectType() instanceof IpPrefixCase)) {
            throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + subobject.getSubobjectType().getClass()
                    + ". Needed IpPrefixCase.");
        }
        final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
        final IpPrefix prefix = specObj.getIpPrefix();

        if (prefix.getIpv6Prefix() == null) {
            throw new IllegalArgumentException("Unknown AbstractPrefix instance. Passed " + prefix.getClass() + ".");
        }
        final byte[] retBytes = new byte[CONTENT6_LENGTH];
        ByteArray.copyWhole(Ipv6Util.bytesForPrefix(prefix.getIpv6Prefix()), retBytes, IP_F_OFFSET);
        retBytes[PREFIX6_F_OFFSET] = UnsignedBytes.checkedCast(Ipv4Util.getPrefixLength(prefix));
        retBytes[ATTRIBUTE6_OFFSET] = UnsignedBytes.checkedCast(subobject.getAttribute().getIntValue());
        return XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), retBytes);
    }
}
