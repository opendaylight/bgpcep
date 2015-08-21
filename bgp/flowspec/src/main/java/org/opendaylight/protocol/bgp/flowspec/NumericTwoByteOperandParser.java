/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericTwoByteValue;

/**
 * Parser class for NumericTwoByteValues.
 */
public final class NumericTwoByteOperandParser extends AbstractNumericOperandParser<NumericTwoByteValue> {

    public static final NumericTwoByteOperandParser INSTANCE;

    static {
        INSTANCE = new NumericTwoByteOperandParser();
    }

    private NumericTwoByteOperandParser() {

    }

    /**
     * Serializes Flowspec component type that has maximum of 2B sized value field and numeric operand.
     *
     * @param list of operands to be serialized
     * @param nlriByteBuf where the operands will be serialized
     */
    @Override
    public final <T extends NumericTwoByteValue> void serialize(final List<T> list, final ByteBuf nlriByteBuf) {
        for (final T operand : list) {
            final ByteBuf protoBuf = Unpooled.buffer();
            Util.writeShortest(operand.getValue(), protoBuf);
            super.serialize(operand.getOp(), protoBuf.readableBytes(), nlriByteBuf);
            nlriByteBuf.writeBytes(protoBuf);
        }
    }

    // TODO: duplicate code with NumericOneByteValue
    @Override
    public final<T extends NumericTwoByteValue> String toString(final List<T> list) {
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
