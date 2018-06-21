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
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;

/**
 * Common parent class for numeric operands.
 *
 * @param <N> numeric operand type
 */
public abstract class AbstractNumericOperandParser<N> extends AbstractOperandParser<NumericOperand> {

    @VisibleForTesting
    public static final String EQUALS_VALUE = "equals";
    @VisibleForTesting
    public static final String GREATER_THAN_VALUE = "greater-than";
    @VisibleForTesting
    public static final String LESS_THAN_VALUE = "less-than";

    private static final int LESS_THAN = 5;
    private static final int GREATER_THAN = 6;
    private static final int EQUAL = 7;

    /**
     * Serializes specific numeric operand type depending on the length field value.
     *
     * @param list of operands to be serialized
     * @param nlriByteBuf where the operands will be serialized
     */
    protected abstract <T extends N> void serialize(final List<T> list, final ByteBuf nlriByteBuf);

    protected abstract <T extends N> String toString(final List<T> list);

    @Override
    public final NumericOperand create(final Set<String> operandValues) {
        return new NumericOperand(
                operandValues.contains(AND_BIT_VALUE),
                operandValues.contains(END_OF_LIST_VALUE),
                operandValues.contains(EQUALS_VALUE),
                operandValues.contains(GREATER_THAN_VALUE),
                operandValues.contains(LESS_THAN_VALUE)
        );
    }

    @Override
    public final void serialize(final NumericOperand operand, final int length,
            final boolean endOfList, final ByteBuf buffer) {
        final BitArray operandValues = new BitArray(OPERAND_LENGTH);
        operandValues.set(END_OF_LIST, endOfList);
        operandValues.set(AND_BIT, operand.isAndBit());
        operandValues.set(LESS_THAN, operand.isLessThan());
        operandValues.set(GREATER_THAN, operand.isGreaterThan());
        operandValues.set(EQUAL, operand.isEquals());
        final byte byteLength = (byte) (Integer.numberOfTrailingZeros(length) << LENGTH_SHIFT);
        buffer.writeByte(operandValues.toByte() | byteLength);
    }

    @Override
    public final NumericOperand parse(final byte operand) {
        final BitArray operandValues = BitArray.valueOf(operand);
        return new NumericOperand(
                operandValues.get(AND_BIT),
                operandValues.get(END_OF_LIST),
                operandValues.get(EQUAL),
                operandValues.get(GREATER_THAN),
                operandValues.get(LESS_THAN)
        );
    }

    @Override
    public String toString(final NumericOperand operand, final boolean isFirst) {
        final StringBuilder buffer = new StringBuilder();
        if (operand.isAndBit()) {
            buffer.append("and ");
        } else if (!isFirst) {
            buffer.append("or ");
        }
        if (operand.isLessThan()) {
            buffer.append("is less than ");
            if (operand.isEquals()) {
                buffer.append("or equals to ");
            }
            return buffer.toString();
        }
        if (operand.isGreaterThan()) {
            buffer.append("is greater than ");
            if (operand.isEquals()) {
                buffer.append("or equals to ");
            }
            return buffer.toString();
        }
        if (operand.isEquals()) {
            buffer.append("equals to ");
        }
        return buffer.toString();
    }
}
