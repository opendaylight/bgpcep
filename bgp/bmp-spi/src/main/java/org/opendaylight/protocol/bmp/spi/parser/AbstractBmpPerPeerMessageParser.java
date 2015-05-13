/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Peer.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.Distinguisher.DistinguisherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader.InboundPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeaderBuilder;

public abstract class AbstractBmpPerPeerMessageParser extends AbstractBmpMessageParser {

    private final MessageRegistry bgpMssageRegistry;

    public AbstractBmpPerPeerMessageParser(final MessageRegistry bgpMssageRegistry) {
        this.bgpMssageRegistry = bgpMssageRegistry;
    }

    protected final PeerHeader parsePerPeerHeader(final ByteBuf bytes) {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final PeerHeaderBuilder phBuilder = new PeerHeaderBuilder();
        phBuilder.setType(Type.forValue(bytes.readByte()));
        final BitArray flags = BitArray.valueOf(bytes, 1);
        phBuilder.setInboundPolicy(InboundPolicy.forValue(flags.get(1) ? 1 : 0));
        bytes.skipBytes(2);
        if (phBuilder.getType().equals(Type.L3vpn)) {
            final DistinguisherType type = DistinguisherType.forValue(bytes.readUnsignedShort());
            //TODO format distinguisher by type to string, read 6 bytes
        } else {
            bytes.skipBytes(8);
        }
        //check v-flag
        if (flags.get(0)) {
            phBuilder.setAddress(new IpAddress(Ipv4Util.addressForByteBuf(bytes)));
            bytes.skipBytes(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
        } else {
            phBuilder.setAddress(new IpAddress(Ipv6Util.addressForByteBuf(bytes)));
        }
        phBuilder.setAs(new AsNumber(bytes.readUnsignedInt()));
        phBuilder.setBgpId(Ipv4Util.addressForByteBuf(bytes));
        phBuilder.setTimestampSec(new Timestamp(bytes.readUnsignedInt()));
        phBuilder.setTimestampMicro(new Timestamp(bytes.readUnsignedInt()));
        return phBuilder.build();
    }

    protected final void serializePerPeerHeader(final PeerHeader peerHeader, final ByteBuf buffer) {
        //TODO
    }

    protected final MessageRegistry getBmpMessageRegistry() {
        return this.bgpMssageRegistry;
    }
}
