/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.NoPathVectorTlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;

/**
 * Parser for {@link NoPathVector}.
 */
public class NoPathVectorTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 1;

    private static final int FLAGS_SIZE = 32;

    private static final int NO_DISJOINT_COMPUTATION = 10;
    private static final int NO_DISJOINT_PATH = 11;
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
        if (buffer.readableBytes() != FLAGS_SIZE / Byte.SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                    + "; Expected: >=" + FLAGS_SIZE / Byte.SIZE + ".");
        }
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        return new NoPathVectorBuilder().setFlags(new Flags(flags.get(CHAIN_UNAVAILABLE),
                flags.get(NO_DISJOINT_COMPUTATION), flags.get(NO_DISJOINT_PATH),
                flags.get(NO_GCO_MIGRATION_PATH), flags.get(NO_GCO_SOLUTION), flags.get(REACHABLITY_PROBLEM),
                flags.get(PATH_KEY), flags.get(PCE_UNAVAILABLE), flags.get(UNKNOWN_DEST), flags.get(UNKNOWN_SRC)))
                .build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof NoPathVector, "NoPathVectorTlv is mandatory.");
        final NoPathVector noPath = (NoPathVector) tlv;
        final ByteBuf body = Unpooled.buffer();
        final BitArray flags = new BitArray(FLAGS_SIZE);
        final Flags f = noPath.getFlags();
        flags.set(REACHABLITY_PROBLEM, f.getP2mpUnreachable());
        flags.set(NO_GCO_SOLUTION, f.getNoGcoSolution());
        flags.set(NO_GCO_MIGRATION_PATH, f.getNoGcoMigration());
        flags.set(PATH_KEY, f.getPathKey());
        flags.set(CHAIN_UNAVAILABLE, f.getChainUnavailable());
        flags.set(UNKNOWN_SRC, f.getUnknownSource());
        flags.set(UNKNOWN_DEST, f.getUnknownDestination());
        flags.set(PCE_UNAVAILABLE, f.getPceUnavailable());
        flags.set(NO_DISJOINT_COMPUTATION, f.getNoDisjointCompute());
        flags.set(NO_DISJOINT_PATH, f.getNoDisjointPath());
        flags.toByteBuf(body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
