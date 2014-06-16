/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;

/**
 * Parser for {@link NoPathVector}
 */
public class NoPathVectorTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 1;

    private static final int FLAGS_F_LENGTH = 4;

    private static final int REACHABLITY_PROBLEM = 24;
    private static final int NO_GCO_SOLUTION = 25;
    private static final int NO_GCO_MIGRATION_PATH = 26;
    private static final int PATH_KEY = 27;
    private static final int CHAIN_UNAVAILABLE = 28;
    private static final int UNKNOWN_SRC = 29;
    private static final int UNKNOWN_DEST = 30;
    private static final int PCE_UNAVAILABLE = 31;

    @Override
    public NoPathVector parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() != FLAGS_F_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >="
                    + FLAGS_F_LENGTH + ".");
        }
        final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readAllBytes(buffer));
        return new NoPathVectorBuilder().setFlags(
                new Flags(flags.get(CHAIN_UNAVAILABLE), flags.get(NO_GCO_MIGRATION_PATH), flags.get(NO_GCO_SOLUTION), flags.get(REACHABLITY_PROBLEM), flags.get(PATH_KEY), flags.get(PCE_UNAVAILABLE), flags.get(UNKNOWN_DEST), flags.get(UNKNOWN_SRC))).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv != null, "NoPathVectorTlv is mandatory.");
        final NoPathVector noPath = (NoPathVector) tlv;
        final ByteBuf body = Unpooled.buffer();
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        Flags f = noPath.getFlags();
        if (f.isP2mpUnreachable() != null) {
            flags.set(REACHABLITY_PROBLEM, f.isP2mpUnreachable());
        }
        if (f.isNoGcoSolution() != null) {
            flags.set(NO_GCO_SOLUTION, f.isNoGcoSolution());
        }
        if (f.isNoGcoMigration() != null) {
            flags.set(NO_GCO_MIGRATION_PATH, f.isNoGcoMigration());
        }
        if (f.isPathKey() != null) {
            flags.set(PATH_KEY, f.isPathKey());
        }
        if (f.isChainUnavailable() != null) {
            flags.set(CHAIN_UNAVAILABLE, f.isChainUnavailable());
        }
        if (f.isUnknownSource() != null) {
            flags.set(UNKNOWN_SRC, f.isUnknownSource());
        }
        if (f.isUnknownDestination() != null) {
            flags.set(UNKNOWN_DEST, f.isUnknownDestination());
        }
        if (f.isPceUnavailable() != null) {
            flags.set(PCE_UNAVAILABLE, f.isPceUnavailable());
        }
        body.writeBytes(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH));
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
