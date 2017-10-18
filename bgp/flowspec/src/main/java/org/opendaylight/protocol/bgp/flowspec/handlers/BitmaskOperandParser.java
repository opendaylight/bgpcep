/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.BitmaskOperand;

/**
 * Parser class for BitmaskOperand.
 */
public final class BitmaskOperandParser extends AbstractOperandParser<BitmaskOperand> {

    public static final BitmaskOperandParser INSTANCE;

    static {
        INSTANCE = new BitmaskOperandParser();
    }

    @VisibleForTesting
    public static final String MATCH_VALUE = "match";
    @VisibleForTesting
    public static final String NOT_VALUE = "not";

    private static final int NOT = 6;
    private static final int MATCH = 7;

    private BitmaskOperandParser() { }

    @Override
    public BitmaskOperand create(final Set<String> opValues) {
        return new BitmaskOperand(
                opValues.contains(AND_BIT_VALUE),
                opValues.contains(END_OF_LIST_VALUE),
                opValues.contains(MATCH_VALUE),
                opValues.contains(NOT_VALUE)
        );
    }

    @Override
    public void serialize(final BitmaskOperand op, final int length, final boolean endOfList,
            final ByteBuf buffer) {
        final BitArray bs = new BitArray(OPERAND_LENGTH);
        bs.set(END_OF_LIST, endOfList);
        bs.set(AND_BIT, op.isAndBit());
        bs.set(MATCH, op.isMatch());
        bs.set(NOT, op.isNot());
        final byte len = (byte) (Integer.numberOfTrailingZeros(length) << LENGTH_SHIFT);
        buffer.writeByte(bs.toByte() | len);
    }

    @Override
    public BitmaskOperand parse(final byte op) {
        final BitArray bs = BitArray.valueOf(op);
        return new BitmaskOperand(bs.get(AND_BIT), bs.get(END_OF_LIST), bs.get(MATCH), bs.get(NOT));
    }

    @Override
    public String toString(final BitmaskOperand op, final boolean isFirst) {
        final StringBuilder buffer = new StringBuilder();
        if (!op.isAndBit() && !isFirst) {
            buffer.append("or ");
        }
        if (op.isAndBit()) {
            buffer.append("and ");
        }
        if (op.isMatch()) {
            buffer.append("does ");
            if (op.isNot()) {
                buffer.append("not ");
            }
            buffer.append("match ");
        } else if (op.isNot()) {
            buffer.append("is not ");
        }
        return buffer.toString();
    }
}
