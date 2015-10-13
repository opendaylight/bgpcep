/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleNlriRegistry implements NlriRegistry {

    private static final int RESERVED = 1;
    private static final int NEXT_HOP_LENGHT = 1;
    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriRegistry.class);

    private final ConcurrentMap<BgpTableType, NlriParser> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends DataObject>, NlriSerializer> serializers = new ConcurrentHashMap<>();
    private final ConcurrentMap<BgpTableType, NextHopParserSerializer> nextHopParsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends CNextHop>, NextHopParserSerializer> nextHopSerializers = new ConcurrentHashMap<>();
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

    synchronized AutoCloseable registerNlriSerializer(final Class<? extends DataObject> nlriClass, final
        NlriSerializer serializer){
        final NlriSerializer prev = this.serializers.get(nlriClass);
        Preconditions.checkState(prev == null, "Serializer already bound to class " + prev);

        this.serializers.put(nlriClass, serializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriRegistry.this.serializers.remove(nlriClass);
                }
            }
        };
    }

    synchronized AutoCloseable registerNlriParser(final Class<? extends AddressFamily> afi,
                                                         final Class<? extends SubsequentAddressFamily> safi, final NlriParser parser,
                                                         final NextHopParserSerializer nextHopSerializer, final Class<? extends CNextHop> cNextHopClass,
                                                         final Class<? extends CNextHop>... cNextHopClassList) {
        final BgpTableType key = createKey(afi, safi);
        final NlriParser prev = this.handlers.get(key);
        Preconditions.checkState(prev == null, "AFI/SAFI is already bound to parser " + prev);

        this.handlers.put(key, parser);
        this.nextHopParsers.put(key,nextHopSerializer);
        this.nextHopSerializers.put(cNextHopClass,nextHopSerializer);
        for (Class<? extends CNextHop> cNextHop : cNextHopClassList) {
            this.nextHopSerializers.put(cNextHop,nextHopSerializer);
        }

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriRegistry.this.handlers.remove(key);
                    SimpleNlriRegistry.this.nextHopParsers.remove(key);
                    SimpleNlriRegistry.this.nextHopSerializers.remove(cNextHopClass);
                    for (Class<? extends CNextHop> cNextHop : cNextHopClassList) {
                        SimpleNlriRegistry.this.nextHopSerializers.remove(cNextHop);
                    }
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
        final int safiVal = buffer.readUnsignedByte();
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
    public void serializeMpReach(final MpReachNlri mpReachNlri, final ByteBuf byteAggregator) {
        byteAggregator.writeShort(this.afiReg.numberForClass(mpReachNlri.getAfi()));
        byteAggregator.writeByte(this.safiReg.numberForClass(mpReachNlri.getSafi()));

        if (mpReachNlri.getCNextHop() != null) {
            final NextHopParserSerializer nextHopSerializer = this.nextHopSerializers.get(mpReachNlri.getCNextHop().getImplementedInterface());
            if (nextHopSerializer != null) {
                final ByteBuf nextHopBuffer = Unpooled.buffer();
                nextHopSerializer.serializeNextHop(mpReachNlri.getCNextHop(), nextHopBuffer);
                byteAggregator.writeByte(nextHopBuffer.writerIndex());
                byteAggregator.writeBytes(nextHopBuffer);
            } else {
                LOG.warn("NexHop Parser/Serializer for AFI/SAFI ({},{}) not bound");
            }
        } else {
            byteAggregator.writeZero(NEXT_HOP_LENGHT);
        }
        byteAggregator.writeZero(RESERVED);
    }

    @Override
    public void serializeMpUnReach(final MpUnreachNlri mpUnreachNlri, final ByteBuf byteAggregator) {
        byteAggregator.writeShort(this.afiReg.numberForClass(mpUnreachNlri.getAfi()));
        byteAggregator.writeByte(this.safiReg.numberForClass(mpUnreachNlri.getSafi()));
    }

    @Override
    public Iterable<NlriSerializer> getSerializers() {
        return Iterables.unmodifiableIterable(this.serializers.values());
    }

    @Override
    public MpReachNlri parseMpReach(final ByteBuf buffer) throws BGPParsingException {
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        builder.setAfi(getAfi(buffer));
        builder.setSafi(getSafi(buffer));

        final BgpTableType key = createKey(builder.getAfi(), builder.getSafi());
        final NlriParser parser = this.handlers.get(key);

        final int nextHopLength = buffer.readUnsignedByte();
        if (nextHopLength != 0) {
            final NextHopParserSerializer nextHopParser = this.nextHopParsers.get(key);
            if (nextHopParser != null) {
                builder.setCNextHop(nextHopParser.parseNextHop(buffer.readSlice(nextHopLength)));
            } else {
                LOG.warn("NexHop Parser/Serializer for AFI/SAFI ({},{}) not bound");
            }
        }
        buffer.skipBytes(RESERVED);

        final ByteBuf nlri = buffer.slice();
        parser.parseNlri(nlri, builder);
        return builder.build();
    }
}
