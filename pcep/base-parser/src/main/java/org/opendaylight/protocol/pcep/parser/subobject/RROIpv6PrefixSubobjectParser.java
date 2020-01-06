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
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

/**
 * Parser for {@link IpPrefixCase}.
 */
public class RROIpv6PrefixSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 2;

    private static final int PREFIX_F_LENGTH = 1;
    private static final int FLAGS_SIZE = 8;

    private static final int PREFIX_F_OFFSET = Ipv6Util.IPV6_LENGTH;

    private static final int CONTENT_LENGTH = Ipv6Util.IPV6_LENGTH + PREFIX_F_LENGTH + FLAGS_SIZE / Byte.SIZE;

    private static final int LPA_F_OFFSET = 7;
    private static final int LPIU_F_OFFSET = 6;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException(
                    "Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final int length = buffer.getUnsignedByte(PREFIX_F_OFFSET);
        final IpPrefixBuilder prefix = new IpPrefixBuilder().setIpPrefix(
                new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(buffer, Ipv6Util.IPV6_LENGTH), length)));
        buffer.skipBytes(PREFIX_F_LENGTH);
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final SubobjectBuilder builder = new SubobjectBuilder()
                .setProtectionAvailable(flags.get(LPA_F_OFFSET))
                .setProtectionInUse(flags.get(LPIU_F_OFFSET))
                .setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(prefix.build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof IpPrefixCase,
                "Unknown subobject instance. Passed %s. Needed IpPrefixCase.", subobject.getSubobjectType().getClass());
        final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
        final Ipv6Prefix ipv6Prefix = specObj.getIpPrefix().getIpv6Prefix();
        checkArgument(ipv6Prefix != null, "Ipv6Prefix is mandatory.");
        serializeSubobject(buffer, subobject, ipv6Prefix);
    }

    static void serializeSubobject(final ByteBuf buffer, final Subobject subobject, final Ipv6Prefix ipv6prefix) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(LPA_F_OFFSET, subobject.isProtectionAvailable());
        flags.set(LPIU_F_OFFSET, subobject.isProtectionInUse());
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        Ipv6Util.writeIpv6Prefix(ipv6prefix, body);
        flags.toByteBuf(body);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}
