/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.protocol.bgp.concepts.util.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

final class SimpleNlriRegistry implements NlriRegistry {

    private final ConcurrentMap<BgpTableType, NlriParser> handlers = new ConcurrentHashMap<>();
    private final SubsequentAddressFamilyRegistry safiReg;
    private final AddressFamilyRegistry afiReg;

    public SimpleNlriRegistry(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
    }

    private static BgpTableType createKey(final Class<? extends AddressFamily> afi,
                                          final Class<? extends SubsequentAddressFamily> safi) {
        Preconditions.checkNotNull(afi);
        Preconditions.checkNotNull(safi);
        return new BgpTableTypeImpl(afi, safi);
    }

    synchronized AutoCloseable registerNlriParser(final Class<? extends AddressFamily> afi,
                                                  final Class<? extends SubsequentAddressFamily> safi, final NlriParser parser) {
        final BgpTableType key = createKey(afi, safi);
        final NlriParser prev = this.handlers.get(key);
        Preconditions.checkState(prev == null, "AFI/SAFI is already bound to parser " + prev);

        this.handlers.put(key, parser);
        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriRegistry.this.handlers.remove(key);
                }
            }
        };
    }

    private Class<? extends AddressFamily> getAfi(final ByteBuf buffer) throws BGPParsingException {
        final int afiVal = buffer.readUnsignedShort();
        final Class<? extends AddressFamily> afi = this.afiReg.classForFamily(afiVal);
        if (afi == null) {
            throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
        }
        return afi;
    }

    private Class<? extends SubsequentAddressFamily> getSafi(final ByteBuf buffer) throws BGPParsingException {
        final int safiVal = UnsignedBytes.toInt(buffer.readByte());
        final Class<? extends SubsequentAddressFamily> safi = this.safiReg.classForFamily(safiVal);
        if (safi == null) {
            throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
        }
        return safi;
    }

    @Override
    public MpUnreachNlri parseMpUnreach(final ByteBuf buffer) throws BGPParsingException {
        final MpUnreachNlriBuilder builder = new MpUnreachNlriBuilder();
        builder.setAfi(getAfi(buffer));
        builder.setSafi(getSafi(buffer));

        final NlriParser parser = this.handlers.get(createKey(builder.getAfi(), builder.getSafi()));
        final ByteBuf nlri = buffer.slice();
        parser.parseNlri(nlri, builder);
        return builder.build();
    }

    @Override
    public void serializeMpReach(MpReachNlri mpReachNlri, ByteBuf byteAggregator) {

        ByteBuf mpReachBuffer = Unpooled.buffer();
        mpReachBuffer.writeShort(this.afiReg.numberForClass(mpReachNlri.getAfi()));
        mpReachBuffer.writeByte(this.safiReg.numberForClass(mpReachNlri.getSafi()));

        ByteBuf nextHopBuffer = Unpooled.buffer();
        NextHopUtil.serializeNextHopSimple(mpReachNlri.getCNextHop(), nextHopBuffer);

        mpReachBuffer.writeByte(nextHopBuffer.writerIndex());
        mpReachBuffer.writeBytes(nextHopBuffer);

        //TODO how to calculate number of SNPAs Subnetwork Points of Attachment and serialize SNPAs ?
        mpReachBuffer.writeByte(0);

        byteAggregator.writeBytes(mpReachBuffer);

    }

    @Override
    public void serializeMpUnReach(MpUnreachNlri mpUnreachNlri, ByteBuf byteAggregator) {

        byteAggregator.writeShort(this.afiReg.numberForClass(mpUnreachNlri.getAfi()));
        byteAggregator.writeByte(this.safiReg.numberForClass(mpUnreachNlri.getSafi()));
    }

    @Override
    public MpReachNlri parseMpReach(final ByteBuf buffer) throws BGPParsingException {
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        builder.setAfi(getAfi(buffer));
        builder.setSafi(getSafi(buffer));

        final NlriParser parser = this.handlers.get(createKey(builder.getAfi(), builder.getSafi()));

        final int nextHopLength = UnsignedBytes.toInt(buffer.readByte());
        final byte[] nextHop = ByteArray.readBytes(buffer, nextHopLength);
        //reserved
        buffer.skipBytes(1);

        final ByteBuf nlri = buffer.slice();
        parser.parseNlri(nlri, nextHop, builder);
        return builder.build();
    }
}
