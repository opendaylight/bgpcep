/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;

/**
 * Common parent class for numeric operands.
 *
 * @param <N> numeric operand type
 */
abstract class AbstractNumericOperandParser<N> extends AbstractOperandParser<NumericOperand> {

    @VisibleForTesting
    static final String EQUALS_VALUE = "equals";
    @VisibleForTesting
    static final String GREATER_THAN_VALUE = "greater-than";
    @VisibleForTesting
    static final String LESS_THAN_VALUE = "less-than";

    protected static final int LESS_THAN = 5;
    protected static final int GREATER_THAN = 6;
    protected static final int EQUAL = 7;

    /**
     * Serializes specific numeric operand type depending on the length field value.
     *
     * @param list of operands to be serialized
     * @param nlriByteBuf where the operands will be serialized
     */
    protected abstract <T extends N> void serialize(final List<T> list, final ByteBuf nlriByteBuf);

    protected abstract <T extends N> String toString(final List<T> list);

    @Override
    protected final NumericOperand create(final Set<String> opValues) {
        return new NumericOperand(opValues.contains(AND_BIT_VALUE), opValues.contains(END_OF_LIST_VALUE), opValues.contains(EQUALS_VALUE), opValues.contains(GREATER_THAN_VALUE), opValues.contains(LESS_THAN_VALUE));
    }

    @Override
    public final void serialize(final NumericOperand op, final int length, final ByteBuf buffer) {
        final BitArray bs = new BitArray(OPERAND_LENGTH);
        bs.set(END_OF_LIST, op.isEndOfList());
        bs.set(AND_BIT, op.isAndBit());
        bs.set(LESS_THAN, op.isLessThan());
        bs.set(GREATER_THAN, op.isGreaterThan());
        bs.set(EQUAL, op.isEquals());
        final byte len = (byte) (Integer.numberOfTrailingZeros(length) << LENGTH_SHIFT);
        buffer.writeByte(bs.toByte() | len);
    }

    @Override
    protected final NumericOperand parse(final byte op) {
        final BitArray bs = BitArray.valueOf(op);
        return new NumericOperand(bs.get(AND_BIT), bs.get(END_OF_LIST), bs.get(EQUAL), bs.get(GREATER_THAN), bs.get(LESS_THAN));
    }

    @Override
    protected String toString(final NumericOperand op, final boolean isFirst) {
        final StringBuilder buffer = new StringBuilder();
        if (!op.isAndBit() && !isFirst) {
            buffer.append("or ");
        }
        if (op.isAndBit()) {
            buffer.append("and ");
        }
        if (op.isLessThan() && op.isEquals()) {
            buffer.append("is less than or equal to ");
            return buffer.toString();
        } else if (op.isGreaterThan() && op.isEquals()) {
            buffer.append("is greater than or equal to ");
            return buffer.toString();
        }
        if (op.isEquals()) {
            buffer.append("equals to ");
        }
        if (op.isLessThan()) {
            buffer.append("is less than ");
        }
        if (op.isGreaterThan()) {
            buffer.append("is greater than ");
        }
        return buffer.toString();
    }
}
