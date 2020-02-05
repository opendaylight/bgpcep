/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv6.ero._case.Ipv6Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv6.ero._case.Ipv6EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.ipv6.ero.backup._case.Ipv6EroBackupBuilder;

public final class Ipv6EroParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    private static final int ERO_IPV6 = 1164;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        return parseIpv6EroCase(slice);
    }

    @Override
    public int getType() {
        return ERO_IPV6;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        checkArgument(bindingSubTlv instanceof Ipv6EroCase, "Wrong BindingSubTlv instance expected",
            bindingSubTlv);
        final Ipv6Ero ipv6Ero = ((Ipv6EroCase) bindingSubTlv).getIpv6Ero();
        TlvUtil.writeTLV(getType(), serializeIpv6EroCase(ipv6Ero.isLoose(), ipv6Ero.getAddress()), aggregator);
    }

    public static Ipv6EroCase parseIpv6EroCase(final ByteBuf buffer) {
        final Ipv6EroBuilder builder = new Ipv6EroBuilder();
        final BitArray flags = BitArray.valueOf(buffer, Ipv4EroParser.FLAGS_SIZE);
        builder.setLoose(flags.get(Ipv4EroParser.LOOSE));
        buffer.skipBytes(Ipv4EroParser.RESERVED_ERO);
        builder.setAddress(Ipv6Util.addressForByteBuf(buffer));
        return new Ipv6EroCaseBuilder().setIpv6Ero(builder.build()).build();
    }

    public static Ipv6EroBackupCase parseIpv6EroBackupCase(final ByteBuf buffer) {
        final Ipv6EroBackupBuilder builder = new Ipv6EroBackupBuilder();
        final BitArray flags = BitArray.valueOf(buffer, Ipv4EroParser.FLAGS_SIZE);
        builder.setLoose(flags.get(Ipv4EroParser.LOOSE));
        buffer.skipBytes(Ipv4EroParser.RESERVED_ERO);
        builder.setAddress(Ipv6Util.addressForByteBuf(buffer));
        return new Ipv6EroBackupCaseBuilder().setIpv6EroBackup(builder.build()).build();
    }

    static ByteBuf serializeIpv6EroCase(final Boolean loose, final Ipv6AddressNoZone address) {
        final ByteBuf buffer = Unpooled.buffer();
        Ipv4EroParser.serializeEroFlags(buffer, loose);
        buffer.writeBytes(Ipv6Util.byteBufForAddress(address));
        return buffer;
    }
}
