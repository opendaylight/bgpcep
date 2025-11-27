/*
 * Copyright (c) 2025 Orange.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.policy.capability.tlv.SrPolicyCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.policy.capability.tlv.SrPolicyCapabilityBuilder;

/**
 * Parser for SR Policy Capability TLV.
 */
public class SrPolicyCapabilityTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 71;

    private static final int SR_POLICY_CAPABILITY_LENGTH = 4;

    // TLVs Flags definition
    private static final int TLV_FLAGS_SIZE = 32;
    private static final int COMPUTATION_PRIORITY = 0;
    private static final int EXPLICIT_NUL_LABEL_POLICY = 1;
    private static final int INVALIDATION = 2;
    private static final int STATELESS_OPERATION = 4;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof SrPolicyCapability, "SrPolicyCapability TLV is mandatory.");
        final SrPolicyCapability srpc = (SrPolicyCapability) tlv;
        final BitArray bs = new BitArray(TLV_FLAGS_SIZE);
        bs.set(COMPUTATION_PRIORITY, srpc.getPriority());
        bs.set(EXPLICIT_NUL_LABEL_POLICY, srpc.getExplicitNull());
        bs.set(INVALIDATION, srpc.getInvalidation());
        bs.set(STATELESS_OPERATION, srpc.getStateless());
        final ByteBuf body = Unpooled.buffer();
        bs.toByteBuf(body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() != SR_POLICY_CAPABILITY_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes());
        }
        final BitArray flags = BitArray.valueOf(buffer, TLV_FLAGS_SIZE);
        return new SrPolicyCapabilityBuilder()
            .setPriority(flags.get(COMPUTATION_PRIORITY))
            .setExplicitNull(flags.get(EXPLICIT_NUL_LABEL_POLICY))
            .setInvalidation(flags.get(INVALIDATION))
            .setStateless(flags.get(STATELESS_OPERATION))
            .build();
    }
}
