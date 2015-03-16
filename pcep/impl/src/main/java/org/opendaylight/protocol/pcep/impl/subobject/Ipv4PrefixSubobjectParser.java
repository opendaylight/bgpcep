/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Prefix;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class Ipv4PrefixSubobjectParser implements SubobjectParser, SubobjectSerializer {

    public static final int TYPE = 1;

    private static final int RESERVED = 1;

    private static final int CONTENT4_LENGTH = 6;

    @Override
    public IpPrefixSubobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT4_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final int length = buffer.getUnsignedByte(Ipv4Util.IP4_LENGTH);
        final IpPrefix prefix = new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH), length));
        return new IpPrefixSubobject() {

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }

            @Override
            public IpPrefix getIpPrefix() {
                return prefix;
            }
        };
    }

    @Override
    public void serializeSubobject(final CSubobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof IpPrefix, "Unknown subobject instance. Passed %s. Needed IpPrefixSubobject.", subobject.getClass());
        final IpPrefixSubobject specObj = (IpPrefixSubobject) subobject;
        final IpPrefix prefix = specObj.getIpPrefix();
        Preconditions.checkArgument(prefix.getIpv4Prefix() != null, "Unknown AbstractPrefix instance. Passed %s.", prefix.getClass());
        final ByteBuf body = Unpooled.buffer(CONTENT4_LENGTH);
        writeIpv4Prefix(prefix.getIpv4Prefix(), body);
        body.writeZero(RESERVED);
    }
}
