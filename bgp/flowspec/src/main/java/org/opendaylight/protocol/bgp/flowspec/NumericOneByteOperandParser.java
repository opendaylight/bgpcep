/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOneByteValue;

/**
 * Parser class for NumericOneByteValues.
 */
public final class NumericOneByteOperandParser extends AbstractNumericOperandParser<NumericOneByteValue> {

    public static final NumericOneByteOperandParser INSTANCE;

    static {
        INSTANCE = new NumericOneByteOperandParser();
    }

    private NumericOneByteOperandParser() {

    }

    /**
     * Serializes Flowspec component type that has maximum of 1B sized value field and numeric operand.
     *
     * @param list of operands to be serialized
     * @param nlriByteBuf where the operands will be serialized
     */
    @Override
    public <T extends NumericOneByteValue> void serialize(final List<T> list, final ByteBuf nlriByteBuf) {
        for (final T operand : list) {
            super.serialize(operand.getOp(), 1, nlriByteBuf);
            Util.writeShortest(operand.getValue(), nlriByteBuf);
        }
    }

    // TODO: duplicate code with NumericTwoByteValue
    @Override
    public <T extends NumericOneByteValue> String toString(final List<T> list) {
        final StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (final T item : list) {
            buffer.append(super.toString(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }
}
