/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.IsisPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.OspfPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.OspfPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.isis.prefix.flags._case.IsisPrefixFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.ospf.prefix.flags._case.OspfPrefixFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.SidLabelIndex;

public final class SrPrefixAttributesParser {

    /* Flags */
    private static final int RE_ADVERTISEMENT = 0;
    private static final int NODE_SID = 1;
    private static final int NO_PHP_OSPF = 1;
    private static final int NO_PHP_ISIS = 2;
    private static final int MAPPING_SERVER = 2;
    private static final int EXPLICIT_NULL = 3;
    private static final int VALUE = 4;
    private static final int LOCAL = 5;
    private static final int FLAGS_SIZE = 8;
    private static final int RESERVED_PREFIX = 2;

    private SrPrefixAttributesParser() {

    }

    public static SrPrefix parseSrPrefix(final ByteBuf buffer, final ProtocolId protocol) {
        final var flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final var builder = new SrPrefixBuilder()
                .setFlags(parsePrefixFlags(flags, protocol))
                .setAlgorithm(Algorithm.forValue(buffer.readUnsignedByte()));
        buffer.skipBytes(RESERVED_PREFIX);
        return builder
            .setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndexByFlags(buffer, flags.get(VALUE), flags.get(LOCAL)))
            .build();
    }

    private static Flags parsePrefixFlags(final BitArray flags, final ProtocolId protocol) {
        return switch (protocol) {
            case IsisLevel1, IsisLevel2 -> new IsisPrefixFlagsCaseBuilder()
                .setIsisPrefixFlags(new IsisPrefixFlagsBuilder()
                    .setReAdvertisement(flags.get(RE_ADVERTISEMENT))
                    .setNodeSid(flags.get(NODE_SID))
                    .setNoPhp(flags.get(NO_PHP_ISIS))
                    .setExplicitNull(flags.get(EXPLICIT_NULL))
                    .setValue(flags.get(VALUE))
                    .setLocal(flags.get(LOCAL))
                    .build())
                .build();
            case Ospf, OspfV3 -> new OspfPrefixFlagsCaseBuilder()
                .setOspfPrefixFlags(new OspfPrefixFlagsBuilder()
                    .setExplicitNull(flags.get(EXPLICIT_NULL))
                    .setMappingServer(flags.get(MAPPING_SERVER))
                    .setNoPhp(flags.get(NO_PHP_OSPF))
                    .setValue(flags.get(VALUE))
                    .setLocal(flags.get(LOCAL))
                    .build())
                .build();
            default -> null;
        };
    }

    public static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf aggregator) {
        serializePrefixAttributes(srPrefix.getFlags(), srPrefix.getAlgorithm(), srPrefix.getSidLabelIndex(),
            aggregator);
    }

    public static void serializePrefixAttributes(final Flags flags, final Algorithm algorithm,
            final SidLabelIndex sidLabelIndex, final ByteBuf buffer) {
        final BitArray bitFlags = serializePrefixFlags(flags, sidLabelIndex);
        bitFlags.toByteBuf(buffer);
        buffer.writeByte(algorithm.getIntValue());
        buffer.writeZero(RESERVED_PREFIX);
        buffer.writeBytes(SidLabelIndexParser.serializeSidValue(sidLabelIndex));
    }

    private static BitArray serializePrefixFlags(final Flags flags, final SidLabelIndex sidLabelIndex) {
        final BitArray bitFlags = new BitArray(FLAGS_SIZE);
        SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE, LOCAL);
        switch (flags) {
            case OspfPrefixFlagsCase ospf -> {
                final var ospfFlags = ospf.getOspfPrefixFlags();
                bitFlags.set(NO_PHP_OSPF, ospfFlags.getNoPhp());
                bitFlags.set(MAPPING_SERVER, ospfFlags.getMappingServer());
                bitFlags.set(EXPLICIT_NULL, ospfFlags.getExplicitNull());
                bitFlags.set(VALUE, ospfFlags.getValue());
                bitFlags.set(LOCAL, ospfFlags.getLocal());
            }
            case IsisPrefixFlagsCase isis -> {
                final var isisFlags = isis.getIsisPrefixFlags();
                bitFlags.set(RE_ADVERTISEMENT, isisFlags.getReAdvertisement());
                bitFlags.set(NODE_SID, isisFlags.getNodeSid());
                bitFlags.set(NO_PHP_ISIS, isisFlags.getNoPhp());
                bitFlags.set(EXPLICIT_NULL, isisFlags.getExplicitNull());
                bitFlags.set(VALUE, isisFlags.getValue());
                bitFlags.set(LOCAL, isisFlags.getLocal());
            }
            case null, default -> {
                // no-op
            }
        }
        return bitFlags;
    }

}
