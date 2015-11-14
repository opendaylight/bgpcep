/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.rro;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Prefix;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

/**
 * Parser for {@link IpPrefixCase}
 */
public class RROIpv4PrefixSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 1;

    private static final int PREFIX_F_LENGTH = 1;
    private static final int FLAGS_SIZE = 8;

    private static final int PREFIX4_F_OFFSET = Ipv4Util.IP4_LENGTH;

    private static final int CONTENT4_LENGTH = Ipv4Util.IP4_LENGTH + PREFIX_F_LENGTH + FLAGS_SIZE / Byte.SIZE;

    private static final int LPA_F_OFFSET = 7;
    private static final int LPIU_F_OFFSET = 6;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT4_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        final int length = buffer.getUnsignedByte(PREFIX4_F_OFFSET);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefix prefix = new IpPrefixBuilder().setIpPrefix(
            new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH), length))).build();
        buffer.skipBytes(PREFIX_F_LENGTH);
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setProtectionAvailable(flags.get(LPA_F_OFFSET));
        builder.setProtectionInUse(flags.get(LPIU_F_OFFSET));
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(prefix).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof IpPrefixCase, "Unknown subobject instance. Passed %s. Needed IpPrefixCase.", subobject.getSubobjectType().getClass());
        final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
        final IpPrefix prefix = specObj.getIpPrefix();
        Preconditions.checkArgument(prefix.getIpv4Prefix() != null || prefix.getIpv6Prefix() != null, "Unknown AbstractPrefix instance. Passed %s.", prefix.getClass());
        if (prefix.getIpv6Prefix() != null) {
            new RROIpv6PrefixSubobjectParser().serializeSubobject(subobject, buffer);
        } else {
            final BitArray flags = new BitArray(FLAGS_SIZE);
            flags.set(LPA_F_OFFSET, subobject.isProtectionAvailable());
            flags.set(LPIU_F_OFFSET, subobject.isProtectionInUse());
            final ByteBuf body = Unpooled.buffer(CONTENT4_LENGTH);
            Preconditions.checkArgument(prefix.getIpv4Prefix() != null, "Ipv4Prefix is mandatory.");
            writeIpv4Prefix(prefix.getIpv4Prefix(), body);
            flags.toByteBuf(body);
            RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
        }
    }
}
