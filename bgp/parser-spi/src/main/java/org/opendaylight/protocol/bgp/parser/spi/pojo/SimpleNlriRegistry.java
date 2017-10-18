/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleNlriRegistry implements NlriRegistry {

    private static final int RESERVED = 1;
    private static final int NEXT_HOP_LENGHT = 1;
    private static final String PARSER_NOT_FOUND = "Nlri parser not found for table type {}";
    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriRegistry.class);

    private final ConcurrentMap<BgpTableType, NlriParser> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends DataObject>, NlriSerializer> serializers = new ConcurrentHashMap<>();
    private final ConcurrentMap<BgpTableType, NextHopParserSerializer> nextHopParsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Entry<Class<? extends CNextHop>, BgpTableType>, NextHopParserSerializer> nextHopSerializers = new ConcurrentHashMap<>();
    private final SubsequentAddressFamilyRegistry safiReg;
    private final AddressFamilyRegistry afiReg;

    public SimpleNlriRegistry(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = requireNonNull(afiReg);
        this.safiReg = requireNonNull(safiReg);
    }

    private static BgpTableType createKey(final Class<? extends AddressFamily> afi,
        final Class<? extends SubsequentAddressFamily> safi) {
        requireNonNull(afi);
        requireNonNull(safi);
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

        if (cNextHopClass != null) {
            final Entry<Class<? extends CNextHop>, BgpTableType> nhKey = new SimpleEntry<>(cNextHopClass, key);
            this.nextHopSerializers.put(nhKey, nextHopSerializer);
            for (final Class<? extends CNextHop> cNextHop : cNextHopClassList) {
                final Entry<Class<? extends CNextHop>, BgpTableType> nhKeys = new SimpleEntry<>(cNextHop, key);
                this.nextHopSerializers.put(nhKeys, nextHopSerializer);
            }
        }

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriRegistry.this.handlers.remove(key);
                    SimpleNlriRegistry.this.nextHopParsers.remove(key);
                    if (cNextHopClass != null) {
                        final Entry<Class<? extends CNextHop>, BgpTableType> nhKey = new SimpleEntry<>(cNextHopClass, key);
                        SimpleNlriRegistry.this.nextHopSerializers.remove(nhKey);
                        for (final Class<? extends CNextHop> cNextHop : cNextHopClassList) {
                            final Entry<Class<? extends CNextHop>, BgpTableType> nhKeys = new SimpleEntry<>(cNextHop, key);
                            SimpleNlriRegistry.this.nextHopSerializers.remove(nhKeys);
                        }
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
    public MpUnreachNlri parseMpUnreach(final ByteBuf buffer, final PeerSpecificParserConstraint constraint)
            throws BGPParsingException {
        final MpUnreachNlriBuilder builder = new MpUnreachNlriBuilder();
        builder.setAfi(getAfi(buffer));
        builder.setSafi(getSafi(buffer));

        final ByteBuf nlri = buffer.slice();
        final BgpTableType key = createKey(builder.getAfi(), builder.getSafi());
        final NlriParser parser = this.handlers.get(key);
        if (parser == null) {
            LOG.warn(PARSER_NOT_FOUND, key);
        } else {
            parser.parseNlri(nlri, builder, constraint);
        }
        return builder.build();
    }

    @Override
    public void serializeMpReach(final MpReachNlri mpReachNlri, final ByteBuf byteAggregator) {
        final Class<? extends AddressFamily> afi = mpReachNlri.getAfi();
        final Class<? extends SubsequentAddressFamily> safi = mpReachNlri.getSafi();
        byteAggregator.writeShort(this.afiReg.numberForClass(afi));
        byteAggregator.writeByte(this.safiReg.numberForClass(safi));

        final CNextHop cNextHop = mpReachNlri.getCNextHop();
        if (cNextHop != null) {
            final Entry<Class<? extends CNextHop>, BgpTableType> key = new SimpleEntry(
                    cNextHop.getImplementedInterface(), new BgpTableTypeImpl(afi, safi));
            final NextHopParserSerializer nextHopSerializer = this.nextHopSerializers.get(key);
            final ByteBuf nextHopBuffer = Unpooled.buffer();
            nextHopSerializer.serializeNextHop(cNextHop, nextHopBuffer);
            byteAggregator.writeByte(nextHopBuffer.writerIndex());
            byteAggregator.writeBytes(nextHopBuffer);

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
    public MpReachNlri parseMpReach(final ByteBuf buffer, final PeerSpecificParserConstraint constraint)
            throws BGPParsingException {
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        final Class<? extends AddressFamily> afi = getAfi(buffer);
        final Class<? extends SubsequentAddressFamily> safi = getSafi(buffer);
        builder.setAfi(afi);
        builder.setSafi(safi);

        final BgpTableType key = createKey(builder.getAfi(), builder.getSafi());

        final int nextHopLength = buffer.readUnsignedByte();
        if (nextHopLength != 0) {
            final NextHopParserSerializer nextHopParser = this.nextHopParsers.get(key);
            if (nextHopParser != null) {
                builder.setCNextHop(nextHopParser.parseNextHop(buffer.readSlice(nextHopLength)));
            } else {
                builder.setCNextHop(NextHopUtil.parseNextHop(buffer.readSlice(nextHopLength)));
                LOG.warn("NexHop Parser/Serializer for AFI/SAFI ({},{}) not bound",afi,safi);
            }
        }
        buffer.skipBytes(RESERVED);

        final ByteBuf nlri = buffer.slice();
        final NlriParser parser = this.handlers.get(key);
        if (parser == null) {
            LOG.warn(PARSER_NOT_FOUND, key);
        } else {
            parser.parseNlri(nlri, builder, constraint);
        }
        return builder.build();
    }
}
