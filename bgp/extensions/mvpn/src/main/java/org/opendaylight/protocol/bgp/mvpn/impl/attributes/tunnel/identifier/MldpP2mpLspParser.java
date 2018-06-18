/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.mvpn.impl.attributes.OpaqueUtil.serializeOpaqueList;
import static org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.parseIpAddress;
import static org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.serializeIpAddress;
import static org.opendaylight.protocol.bgp.mvpn.spi.pojo.attributes.tunnel.identifier.SimpleTunnelIdentifierRegistry.NO_TUNNEL_INFORMATION_PRESENT;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.OpaqueUtil;
import org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier.AbstractTunnelIdentifier;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.PmsiTunnelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpP2mpLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpP2mpLspBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MldpP2mpLspParser extends AbstractTunnelIdentifier<MldpP2mpLsp> {
    private static final Logger LOG = LoggerFactory.getLogger(MldpP2mpLspParser.class);
    private static final short P2MP_TYPE = 6;
    private static final int RESERVED = 1;
    private final AddressFamilyRegistry addressFamilyRegistry;

    public MldpP2mpLspParser(final AddressFamilyRegistry addressFamilyRegistry) {
        this.addressFamilyRegistry = addressFamilyRegistry;
    }

    @Override
    public int serialize(final MldpP2mpLsp tunnelIdentifier, final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.mldp.p2mp.lsp.MldpP2mpLsp mldpP2mpLsp = tunnelIdentifier.getMldpP2mpLsp();

        final ByteBuf opaqueValues = Unpooled.buffer();
        final int addressFamily = getAddressFamilyValue(mldpP2mpLsp.getAddressFamily());

        if (!serializeOpaqueList(mldpP2mpLsp.getOpaqueValue(), opaqueValues) || addressFamily == 0) {
            return NO_TUNNEL_INFORMATION_PRESENT;
        }
        final IpAddress rootNode = mldpP2mpLsp.getRootNodeAddress();
        ByteBufWriteUtil.writeUnsignedByte(P2MP_TYPE, buffer);
        ByteBufWriteUtil.writeUnsignedShort(addressFamily, buffer);
        ByteBufWriteUtil.writeUnsignedByte(getAdressFamilyLength(rootNode), buffer);
        serializeIpAddress(rootNode, buffer);

        ByteBufWriteUtil.writeUnsignedShort(opaqueValues.readableBytes(), buffer);
        buffer.writeBytes(opaqueValues);
        return getType();
    }

    @Override
    public Class<? extends TunnelIdentifier> getClazz() {
        return MldpP2mpLsp.class;
    }

    @Override
    public int getType() {
        return PmsiTunnelType.MldpP2mpLsp.getIntValue();
    }

    private static short getAdressFamilyLength(final IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() == null) {
            return Ipv6Util.IPV6_LENGTH;
        }
        return Ipv4Util.IP4_LENGTH;
    }

    private int getAddressFamilyValue(final Class<? extends AddressFamily> addressFamily) {
        final Integer type = this.addressFamilyRegistry.numberForClass(addressFamily);
        if (type == null) {
            return 0;
        }
        return type;
    }

    @Override
    public MldpP2mpLsp parse(final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.mldp.p2mp.lsp.MldpP2mpLspBuilder mldpP2mpLsp =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.MldpP2mpLspBuilder();
        buffer.skipBytes(RESERVED);
        final Class<? extends AddressFamily> addressFamily = this.addressFamilyRegistry
                .classForFamily(buffer.readUnsignedShort());
        if (addressFamily == null) {
            LOG.debug("Skipping serialization of TunnelIdentifier {}, address family type  supported",
                    ByteBufUtil.hexDump(buffer));
            return null;
        }
        mldpP2mpLsp.setAddressFamily(addressFamily);
        final short rootNodeLength = buffer.readUnsignedByte();
        mldpP2mpLsp.setRootNodeAddress(parseIpAddress(rootNodeLength, buffer.readBytes(rootNodeLength)));
        final int opaqueValueLength = buffer.readUnsignedShort();
        mldpP2mpLsp.setOpaqueValue(OpaqueUtil.parseOpaqueList(buffer.readBytes(opaqueValueLength)));
        return new MldpP2mpLspBuilder().setMldpP2mpLsp(mldpP2mpLsp.build()).build();
    }
}
