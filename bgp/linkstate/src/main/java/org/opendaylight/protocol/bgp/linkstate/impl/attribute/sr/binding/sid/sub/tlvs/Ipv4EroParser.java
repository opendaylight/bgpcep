/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCaseBuilder;

public final class Ipv4EroParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    private static final int ERO_IPV4 = 1163;
    static final int FLAGS_SIZE = 8;
    static final int LOOSE = 0;
    static final int RESERVED_ERO = 3;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        final Ipv4EroCase ipv4Ero = parseIpv4EroCase(slice);
        return new Ipv4EroCaseBuilder().setAddress(ipv4Ero.getAddress()).setLoose(ipv4Ero.isLoose()).build();
    }

    @Override
    public int getType() {
        return ERO_IPV4;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        Preconditions.checkArgument(bindingSubTlv instanceof Ipv4EroCase, "Wrong BindingSubTlv instance expected", bindingSubTlv);
        final Ipv4EroCase ipv4Ero = (Ipv4EroCase) bindingSubTlv;
        TlvUtil.writeTLV(getType(), serializeIpv4EroCase(ipv4Ero.isLoose(), ipv4Ero.getAddress()), aggregator);
    }

    static Ipv4EroCase parseIpv4EroCase(final ByteBuf buffer) {
        final Ipv4EroCaseBuilder builder = new Ipv4EroCaseBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setLoose(flags.get(LOOSE));
        buffer.skipBytes(RESERVED_ERO);
        builder.setAddress(Ipv4Util.addressForByteBuf(buffer));
        return builder.build();
    }


    static ByteBuf serializeIpv4EroCase(final Boolean loose, final Ipv4Address address) {
        final ByteBuf buffer = Unpooled.buffer();
        serializeEroFlags(buffer, loose);
        buffer.writeBytes(Ipv4Util.byteBufForAddress(address));
        return buffer;
    }

    static void serializeEroFlags(final ByteBuf buffer, final Boolean loose) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(LOOSE, loose);
        flags.toByteBuf(buffer);
        buffer.writeZero(RESERVED_ERO);
    }
}
