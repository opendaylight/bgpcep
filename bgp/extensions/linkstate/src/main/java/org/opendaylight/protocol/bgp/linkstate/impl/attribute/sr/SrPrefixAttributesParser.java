/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.IsisPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.OspfPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.OspfPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;

public final class SrPrefixAttributesParser {

    /* Flags */
    private static final int RE_ADVERTISEMENT = 0;
    private static final int NODE_SID = 1;
    private static final int NO_PHP_OSPF = 1;
    private static final int NO_PHP = 2;
    private static final int MAPPING_SERVER = 2;
    private static final int EXPLICIT_NULL = 3;
    private static final int VALUE = 4;
    private static final int LOCAL = 5;
    private static final int FLAGS_SIZE = 8;
    private static final int RESERVED_PREFIX = 2;

    private SrPrefixAttributesParser() {
        throw new UnsupportedOperationException();
    }

    public static SrPrefix parseSrPrefix(final ByteBuf buffer, final ProtocolId protocol) {
        final SrPrefixBuilder builder = new SrPrefixBuilder();
        builder.setFlags(parsePrefixFlags(BitArray.valueOf(buffer, FLAGS_SIZE), protocol));
        builder.setAlgorithm(Algorithm.forValue(buffer.readUnsignedByte()));
        buffer.skipBytes(RESERVED_PREFIX);
        builder.setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer));
        return builder.build();
    }

    private static Flags parsePrefixFlags(final BitArray flags, final ProtocolId protocol) {
        switch (protocol) {
        case IsisLevel1:
        case IsisLevel2:
            return new IsisPrefixFlagsCaseBuilder().setReadvertisement(flags.get(RE_ADVERTISEMENT))
                .setNodeSid(flags.get(NODE_SID)).setNoPhp(flags.get(NO_PHP)).setExplicitNull(flags.get(EXPLICIT_NULL)).build();
        case Ospf:
        case OspfV3:
            return new OspfPrefixFlagsCaseBuilder().setExplicitNull(flags.get(EXPLICIT_NULL))
                .setMappingServer(flags.get(MAPPING_SERVER)).setNoPhp(flags.get(NO_PHP_OSPF)).build();
        default:
            return null;
        }
    }

    public static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf aggregator) {
        serializePrefixAttributes(srPrefix.getFlags(), srPrefix.getAlgorithm(), srPrefix.getSidLabelIndex(), aggregator);
    }

    public static void serializePrefixAttributes(final Flags flags, final Algorithm algorithm, final SidLabelIndex sidLabelIndex, final ByteBuf buffer) {
        final BitArray bitFlags = serializePrefixFlags(flags, sidLabelIndex);
        bitFlags.toByteBuf(buffer);
        buffer.writeByte(algorithm.getIntValue());
        buffer.writeZero(RESERVED_PREFIX);
        buffer.writeBytes(SidLabelIndexParser.serializeSidValue(sidLabelIndex));
    }

    private static BitArray serializePrefixFlags(final Flags flags, final SidLabelIndex sidLabelIndex) {
        final BitArray bitFlags = new BitArray(FLAGS_SIZE);
        SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE, LOCAL);
        if (flags instanceof OspfPrefixFlagsCase) {
            final OspfPrefixFlagsCase ospfFlags = (OspfPrefixFlagsCase) flags;
            bitFlags.set(NO_PHP_OSPF, ospfFlags.isNoPhp());
            bitFlags.set(MAPPING_SERVER, ospfFlags.isMappingServer());
            bitFlags.set(EXPLICIT_NULL, ospfFlags.isExplicitNull());
        } else if (flags instanceof IsisPrefixFlagsCase) {
            final IsisPrefixFlagsCase isisFlags = (IsisPrefixFlagsCase) flags;
            bitFlags.set(RE_ADVERTISEMENT, isisFlags.isReadvertisement());
            bitFlags.set(NODE_SID, isisFlags.isNodeSid());
            bitFlags.set(NO_PHP, isisFlags.isNoPhp());
            bitFlags.set(EXPLICIT_NULL, isisFlags.isExplicitNull());
        }
        return bitFlags;
    }

}
