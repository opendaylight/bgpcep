/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.IsisPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.OspfPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.prefix.sid.tlv.flags.OspfPrefixFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;

public final class SrPrefixAttributesParser {

    private static final int FLAGS_SIZE = 8;

    private SrPrefixAttributesParser() {
        throw new UnsupportedOperationException();
    }

    /* Flags */
    private static final int RE_ADVERTISEMENT = 0;
    private static final int NODE_SID = 1;
    private static final int NO_PHP_OSPF = 1;
    private static final int NO_PHP = 2;
    private static final int MAPPING_SERVER = 2;
    private static final int EXPLICIT_NULL = 3;
    private static final int VALUE = 4;
    private static final int LOCAL = 5;

    public static SrPrefix parseSrPrefix(final ByteBuf buffer, final ProtocolId protocol) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final SrPrefixBuilder builder = new SrPrefixBuilder();
        builder.setFlags(parseFlags(flags, protocol));
        builder.setAlgorithm(Algorithm.forValue(buffer.readUnsignedByte()));
        final int sidLength = SidLabelIndexParser.getLength(flags, VALUE, LOCAL);
        builder.setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(sidLength, buffer));
        return builder.build();
    }

    private static Flags parseFlags(final BitArray flags, final ProtocolId protocol) {
        if (protocol.equals(ProtocolId.IsisLevel1) || protocol.equals(ProtocolId.IsisLevel2)) {
            return new IsisPrefixFlagsCaseBuilder()
                .setReadvertisement(flags.get(RE_ADVERTISEMENT))
                .setNodeSid(flags.get(NODE_SID))
                .setNoPhp(flags.get(NO_PHP))
                .setExplicitNull(flags.get(EXPLICIT_NULL)).build();
        }
        if (protocol.equals(ProtocolId.Ospf)) {
            return new OspfPrefixFlagsCaseBuilder()
                .setExplicitNull(flags.get(EXPLICIT_NULL))
                .setMappingServer(flags.get(MAPPING_SERVER))
                .setNoPhp(flags.get(NO_PHP_OSPF)).build();
        }
        return null;
    }

    public static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf buffer) {
        final Flags flags = srPrefix.getFlags();
        final BitArray bs = serializePrefixFlags(flags, srPrefix.getSidLabelIndex());
        bs.toByteBuf(buffer);
        buffer.writeByte(srPrefix.getAlgorithm().getIntValue());
        buffer.writeBytes(SidLabelIndexParser.serializeSidValue(srPrefix.getSidLabelIndex()));
    }

    private static BitArray serializePrefixFlags(final Flags flags, final SidLabelIndex sidValue) {
        final BitArray bitFlags = new BitArray(FLAGS_SIZE);
        SidLabelIndexParser.setFlags(sidValue, bitFlags, VALUE, LOCAL);
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
