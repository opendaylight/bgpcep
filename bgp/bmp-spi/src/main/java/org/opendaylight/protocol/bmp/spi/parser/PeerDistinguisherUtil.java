/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.DistinguisherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.Distinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.DistinguisherBuilder;


/**
 * Route Distinguisher encoding/decoding utility
 * @see <a href="https://tools.ietf.org/html/rfc4364#section-4.2">https://tools.ietf.org/html/rfc4364#section-4.2</a>
 *
 */
@Deprecated
public final class PeerDistinguisherUtil {

    private static final class PeerDistinguisher {

        private static final String COLON = ":";

        private final String administratorSubfield;
        private final long assignedNumber;

        private PeerDistinguisher(final String administratorSubfield, final long assignedNumber) {
            this.administratorSubfield = administratorSubfield;
            this.assignedNumber = assignedNumber;
        }

        public static String toString(final String administratorSubfield, final long assignedNumber) {
            Preconditions.checkArgument(administratorSubfield != null);
            return administratorSubfield + COLON + assignedNumber;
        }

        public static PeerDistinguisher fromString(final String distinguisher) {
            Preconditions.checkArgument(distinguisher != null);
            final String[] strings = distinguisher.split(COLON);
            Preconditions.checkState(strings.length == 2);
            return new PeerDistinguisher(strings[0], Long.parseLong(strings[1]));
        }

    }

    private PeerDistinguisherUtil() {
        throw new UnsupportedOperationException();
    }

    public static Distinguisher parsePeerDistingisher(final ByteBuf buffer) {
        final DistinguisherBuilder distinguisherBuilder = new DistinguisherBuilder();
        final DistinguisherType type = DistinguisherType.forValue(buffer.readUnsignedShort());
        distinguisherBuilder.setDistinguisherType(type);
        switch (type) {
        case Type0:
            final int asNumber = buffer.readUnsignedShort();
            distinguisherBuilder.setDistinguisher(PeerDistinguisher.toString(Integer.toString(asNumber), buffer.readUnsignedInt()));
            break;
        case Type1:
            final Ipv4Address ipv4Address = Ipv4Util.addressForByteBuf(buffer);
            distinguisherBuilder.setDistinguisher(PeerDistinguisher.toString(ipv4Address.getValue(), buffer.readUnsignedShort()));
            break;
        case Type2:
            final long asNumber4Bytes = buffer.readUnsignedInt();
            distinguisherBuilder.setDistinguisher(PeerDistinguisher.toString(Long.toString(asNumber4Bytes), buffer.readUnsignedShort()));
            break;
        default:
            break;
        }
        return distinguisherBuilder.build();
    }

    public static void serializePeerDistinguisher(final Distinguisher distinguisher, final ByteBuf output) {
        Preconditions.checkArgument(distinguisher != null && distinguisher.getDistinguisherType() != null && distinguisher.getDistinguisher() != null);
        ByteBufWriteUtil.writeUnsignedShort(distinguisher.getDistinguisherType().getIntValue(), output);
        final PeerDistinguisher peerDistinguisher = PeerDistinguisher.fromString(distinguisher.getDistinguisher());
        switch (distinguisher.getDistinguisherType()) {
        case Type0:
            ByteBufWriteUtil.writeUnsignedShort(Integer.valueOf(peerDistinguisher.administratorSubfield), output);
            ByteBufWriteUtil.writeUnsignedInt(peerDistinguisher.assignedNumber, output);
            break;
        case Type1:
            ByteBufWriteUtil.writeIpv4Address(new Ipv4Address(peerDistinguisher.administratorSubfield), output);
            ByteBufWriteUtil.writeUnsignedShort(Ints.checkedCast(peerDistinguisher.assignedNumber), output);
            break;
        case Type2:
            ByteBufWriteUtil.writeUnsignedInt(Long.valueOf(peerDistinguisher.administratorSubfield), output);
            ByteBufWriteUtil.writeUnsignedShort(Ints.checkedCast(peerDistinguisher.assignedNumber), output);
            break;
        default:
            break;
        }
    }
}
