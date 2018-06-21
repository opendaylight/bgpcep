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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCaseBuilder;

public final class UnnumberedEroParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    private static final int UNNUMBERED_ERO = 1165;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        final UnnumberedInterfaceIdEroCase unnumbered = parseUnnumberedEroCase(slice);
        return new UnnumberedInterfaceIdEroCaseBuilder().setLoose(unnumbered.isLoose()).setRouterId(unnumbered.getRouterId())
            .setInterfaceId(unnumbered.getInterfaceId()).build();
    }

    @Override
    public int getType() {
        return UNNUMBERED_ERO;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        Preconditions.checkArgument(bindingSubTlv instanceof UnnumberedInterfaceIdEroCase, "Wrong BindingSubTlv instance expected", bindingSubTlv);
        final UnnumberedInterfaceIdEroCase unnumberedEro = (UnnumberedInterfaceIdEroCase) bindingSubTlv;
        TlvUtil.writeTLV(getType(), serializeUnnumberedIdEro(unnumberedEro.isLoose(), unnumberedEro.getRouterId(), unnumberedEro.getInterfaceId()), aggregator);
    }

    static UnnumberedInterfaceIdEroCase parseUnnumberedEroCase(final ByteBuf buffer) {
        final UnnumberedInterfaceIdEroCaseBuilder builder = new UnnumberedInterfaceIdEroCaseBuilder();
        final BitArray flags = BitArray.valueOf(buffer, Ipv4EroParser.FLAGS_SIZE);
        builder.setLoose(flags.get(Ipv4EroParser.LOOSE));
        buffer.skipBytes(Ipv4EroParser.RESERVED_ERO);
        builder.setRouterId(buffer.readUnsignedInt());
        builder.setInterfaceId(buffer.readUnsignedInt());
        return builder.build();
    }

    static ByteBuf serializeUnnumberedIdEro(final Boolean loose, final Long routerId, final Long interfaceId) {
        final ByteBuf buffer = Unpooled.buffer();
        Ipv4EroParser.serializeEroFlags(buffer, loose);
        buffer.writeInt(routerId.intValue());
        buffer.writeInt(interfaceId.intValue());
        return buffer;
    }
}
