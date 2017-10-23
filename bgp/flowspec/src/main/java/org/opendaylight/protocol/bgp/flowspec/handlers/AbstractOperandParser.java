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

/**
 * Top-level abstract class for all defined operands.
 *
 * @param <T> operand Type
 */
public abstract class AbstractOperandParser<T> {

    @VisibleForTesting
    public static final String AND_BIT_VALUE = "and-bit";
    @VisibleForTesting
    public static final String END_OF_LIST_VALUE = "end-of-list";

    protected static final int OPERAND_LENGTH = 8;

    protected static final int END_OF_LIST = 0;
    protected static final int AND_BIT = 1;

    protected static final int LENGTH_SHIFT = 4;

    private static final int LENGTH_BITMASK = 48;

    @VisibleForTesting
    public static final short parseLength(final byte op) {
        return (short) (1 << ((op & LENGTH_BITMASK) >> LENGTH_SHIFT));
    }

    /**
     * Creates operand from a set of operand values.
     *
     * @param opValues set of operand values
     * @return specific type of operand
     */
    protected abstract T create(final Set<String> opValues);

    /**
     * Serializes operand to bytes.
     *
     * @param op operand to be serialized
     * @param length value of the 'length' field
     * @param endOfList if this operand is at the end of the list
     * @param buffer where the operand will be serialized to
     */
    protected abstract void serialize(final T op, final int length, final boolean endOfList,
            final ByteBuf buffer);

    /**
     * Parses operand from byte value.
     *
     * @param op byte representation of an operand
     * @return operand object
     */
    protected abstract T parse(final byte op);

    /**
     * Creates a string representation of the operand.
     * E.g. : 'and does not match'
     *
     * @param op operand
     * @param isFirst true if this operand is the first in list of operands
     * @return String representation of the operand
     */
    protected abstract String toString(final T op, final boolean isFirst);
}
