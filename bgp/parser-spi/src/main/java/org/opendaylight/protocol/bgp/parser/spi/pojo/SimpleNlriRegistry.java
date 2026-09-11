/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleNlriRegistry implements NlriRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriRegistry.class);
    private static final int RESERVED = 1;
    private static final String PARSER_NOT_FOUND = "Nlri parser not found for table type {}";

    private final ConcurrentHashMap<BgpTableType, NlriParser> handlers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends DataObject>, NlriSerializer> serializers =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BgpTableType, NextHopParserSerializer> nextHopParsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Map.Entry<Class<? extends CNextHop>, BgpTableType>, NextHopParserSerializer>
        nextHopSerializers = new ConcurrentHashMap<>();
    private final SubsequentAddressFamilyRegistry safiReg;
    private final AddressFamilyRegistry afiReg;

    SimpleNlriRegistry(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = requireNonNull(afiReg);
        this.safiReg = requireNonNull(safiReg);
    }

    private static BgpTableType createKey(final AddressFamily afi, final SubsequentAddressFamily safi) {
        return new BgpTableTypeImpl(requireNonNull(afi, "afi"), requireNonNull(safi, "safi"));
    }

    synchronized Registration registerNlriSerializer(final Class<? extends DataObject> nlriClass,
            final NlriSerializer serializer) {
        final var prev = serializers.putIfAbsent(nlriClass, serializer);
        if (prev != null) {
            throw new IllegalStateException("Serializer already bound to class " + prev);
        }

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (SimpleNlriRegistry.this) {
                    serializers.remove(nlriClass);
                }
            }
        };
    }

    synchronized Registration registerNlriParser(final AddressFamily afi, final SubsequentAddressFamily safi,
            final NlriParser parser, final NextHopParserSerializer nextHopSerializer,
            final Class<? extends CNextHop> cnextHopClass, final Class<? extends CNextHop>... cnextHopClassList) {
        final var key = createKey(afi, safi);
        final var prev = handlers.putIfAbsent(key, parser);
        if (prev != null) {
            throw new IllegalStateException("AFI/SAFI is already bound to parser " + prev);
        }
        nextHopParsers.put(key, nextHopSerializer);

        if (cnextHopClass != null) {
            nextHopSerializers.put(Map.entry(cnextHopClass, key), nextHopSerializer);
            for (var cnextHop : cnextHopClassList) {
                nextHopSerializers.put(Map.entry(cnextHop, key), nextHopSerializer);
            }
        }

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (SimpleNlriRegistry.this) {
                    handlers.remove(key);
                    nextHopParsers.remove(key);
                    if (cnextHopClass != null) {
                        nextHopSerializers.remove(Map.entry(cnextHopClass, key));
                        for (var cnextHop : cnextHopClassList) {
                            nextHopSerializers.remove(Map.entry(cnextHop, key));
                        }
                    }
                }
            }
        };
    }

    private AddressFamily getAfi(final ByteBuf buffer) throws BGPParsingException {
        final int afiVal = buffer.readUnsignedShort();
        final var afi = afiReg.classForFamily(afiVal);
        if (afi == null) {
            throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
        }
        return afi;
    }

    private SubsequentAddressFamily getSafi(final ByteBuf buffer) throws BGPParsingException {
        final int safiVal = buffer.readUnsignedByte();
        final var safi = safiReg.classForFamily(safiVal);
        if (safi == null) {
            throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
        }
        return safi;
    }

    @Override
    public MpUnreachNlri parseMpUnreach(final ByteBuf buffer, final PeerSpecificParserConstraint constraint)
            throws BGPParsingException {
        final var afi = getAfi(buffer);
        final var safi = getSafi(buffer);
        final var builder = new MpUnreachNlriBuilder()
            .setAfi(afi)
            .setSafi(safi);

        if (buffer.isReadable()) {
            final var nlri = buffer.slice();
            final var key = createKey(afi, safi);
            final var parser = handlers.get(key);
            if (parser != null) {
                parser.parseNlri(nlri, builder, constraint);
            } else {
                LOG.warn(PARSER_NOT_FOUND, key);
            }
        }
        return builder.build();
    }

    @Override
    public void serializeMpReach(final MpReachNlri mpReachNlri, final ByteBuf byteAggregator) {
        final var afi = mpReachNlri.getAfi();
        final var safi = mpReachNlri.getSafi();
        byteAggregator.writeShort(afiReg.numberForClass(afi));
        byteAggregator.writeByte(safiReg.numberForClass(safi));

        final var cNextHop = mpReachNlri.getCNextHop();
        if (cNextHop != null) {
            final var nextHopSerializer = nextHopSerializers.get(
                Map.entry(cNextHop.implementedInterface(), new BgpTableTypeImpl(afi, safi)));
            final var nextHopBuffer = Unpooled.buffer();
            nextHopSerializer.serializeNextHop(cNextHop, nextHopBuffer);
            byteAggregator.writeByte(nextHopBuffer.writerIndex());
            byteAggregator.writeBytes(nextHopBuffer);

        } else {
            byteAggregator.writeByte(0);
        }
        byteAggregator.writeZero(RESERVED);
    }

    @Override
    public void serializeMpUnReach(final MpUnreachNlri mpUnreachNlri, final ByteBuf byteAggregator) {
        byteAggregator.writeShort(afiReg.numberForClass(mpUnreachNlri.getAfi()));
        byteAggregator.writeByte(safiReg.numberForClass(mpUnreachNlri.getSafi()));
    }

    @Override
    public Iterable<NlriSerializer> getSerializers() {
        return Collections.unmodifiableCollection(serializers.values());
    }

    @Override
    public MpReachNlri parseMpReach(final ByteBuf buffer, final PeerSpecificParserConstraint constraint)
            throws BGPParsingException {
        final var afi = getAfi(buffer);
        final var safi = getSafi(buffer);
        final var key = createKey(afi, safi);
        final var builder = new MpReachNlriBuilder()
            .setAfi(afi)
            .setSafi(safi);

        final int nextHopLength = buffer.readUnsignedByte();
        if (nextHopLength != 0) {
            final var nextHopParser = nextHopParsers.get(key);
            if (nextHopParser != null) {
                builder.setCNextHop(nextHopParser.parseNextHop(buffer.readSlice(nextHopLength)));
            } else {
                builder.setCNextHop(NextHopUtil.parseNextHop(buffer.readSlice(nextHopLength)));
                LOG.warn("NexHop Parser/Serializer for AFI/SAFI ({},{}) not bound",afi,safi);
            }
        }
        buffer.skipBytes(RESERVED);

        final var nlri = buffer.slice();
        final var parser = handlers.get(key);
        if (parser == null) {
            LOG.warn(PARSER_NOT_FOUND, key);
        } else {
            parser.parseNlri(nlri, builder, constraint);
        }
        return builder.build();
    }

    @Override
    public Optional<MpUnreachNlri> convertMpReachToMpUnReach(final MpReachNlri mpReachNlri,
            final MpUnreachNlri mpUnreachNlri) {
        if (mpUnreachNlri == null) {
            return Optional.of(new MpUnreachNlriBuilder()
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder()
                    .setDestinationType(mpReachNlri.getAdvertizedRoutes().getDestinationType())
                    .build())
                .build());
        }

        final var key = createKey(mpUnreachNlri.getAfi(), mpUnreachNlri.getSafi());
        final var parser = handlers.get(key);
        if (parser == null) {
            LOG.debug("Parser for {} not found", key);
            return Optional.empty();
        }

        final var builder = new MpUnreachNlriBuilder(mpUnreachNlri);
        return parser.convertMpReachToMpUnReach(mpReachNlri, builder) ? Optional.of(builder.build()) : Optional.empty();
    }
}
