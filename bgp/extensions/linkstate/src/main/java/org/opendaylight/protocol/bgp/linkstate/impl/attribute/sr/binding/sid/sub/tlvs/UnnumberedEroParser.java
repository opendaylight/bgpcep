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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.unnumbered._interface.id.backup.ero._case.UnnumberedInterfaceIdBackupEroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.unnumbered._interface.id.ero._case.UnnumberedInterfaceIdEro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.unnumbered._interface.id.ero._case.UnnumberedInterfaceIdEroBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class UnnumberedEroParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    private static final int UNNUMBERED_ERO = 1165;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        return parseUnnumberedEroCase(slice);
    }

    @Override
    public int getType() {
        return UNNUMBERED_ERO;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        checkArgument(bindingSubTlv instanceof UnnumberedInterfaceIdEroCase,
            "Wrong BindingSubTlv instance expected", bindingSubTlv);
        final UnnumberedInterfaceIdEro unnumberedEro =
                ((UnnumberedInterfaceIdEroCase) bindingSubTlv).getUnnumberedInterfaceIdEro();
        TlvUtil.writeTLV(getType(), serializeUnnumberedIdEro(unnumberedEro.isLoose(), unnumberedEro.getRouterId(),
            unnumberedEro.getInterfaceId()), aggregator);
    }

    static UnnumberedInterfaceIdEroCase parseUnnumberedEroCase(final ByteBuf buffer) {
        final UnnumberedInterfaceIdEroBuilder builder = new UnnumberedInterfaceIdEroBuilder();
        final BitArray flags = BitArray.valueOf(buffer, Ipv4EroParser.FLAGS_SIZE);
        builder.setLoose(flags.get(Ipv4EroParser.LOOSE));
        buffer.skipBytes(Ipv4EroParser.RESERVED_ERO);
        builder.setRouterId(ByteBufUtils.readUint32(buffer));
        builder.setInterfaceId(ByteBufUtils.readUint32(buffer));
        return new UnnumberedInterfaceIdEroCaseBuilder().setUnnumberedInterfaceIdEro(builder.build()).build();
    }

    static UnnumberedInterfaceIdBackupEroCase parseUnnumberedEroBackupCase(final ByteBuf buffer) {
        final UnnumberedInterfaceIdBackupEroBuilder builder = new UnnumberedInterfaceIdBackupEroBuilder();
        final BitArray flags = BitArray.valueOf(buffer, Ipv4EroParser.FLAGS_SIZE);
        builder.setLoose(flags.get(Ipv4EroParser.LOOSE));
        buffer.skipBytes(Ipv4EroParser.RESERVED_ERO);
        builder.setRouterId(ByteBufUtils.readUint32(buffer));
        builder.setInterfaceId(ByteBufUtils.readUint32(buffer));
        return new UnnumberedInterfaceIdBackupEroCaseBuilder().setUnnumberedInterfaceIdBackupEro(builder.build())
                .build();
    }

    static ByteBuf serializeUnnumberedIdEro(final Boolean loose, final Uint32 routerId, final Uint32 interfaceId) {
        final ByteBuf buffer = Unpooled.buffer();
        Ipv4EroParser.serializeEroFlags(buffer, loose);
        buffer.writeInt(routerId.intValue());
        buffer.writeInt(interfaceId.intValue());
        return buffer;
    }
}
