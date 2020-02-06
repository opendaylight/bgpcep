/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.NumericOneByteValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.NumericOperand;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser class for NumericOneByteValues.
 */
public final class NumericOneByteOperandParser extends AbstractNumericByteOperandParser<NumericOneByteValue, Uint8> {
    public static final NumericOneByteOperandParser INSTANCE = new NumericOneByteOperandParser();

    private NumericOneByteOperandParser() {

    }

    /**
     * Serializes Flowspec component type that has maximum of 1B sized value field and numeric operand.
     *
     * @param list        of operands to be serialized
     * @param nlriByteBuf where the operands will be serialized
     */
    @Override
    public <T extends NumericOneByteValue> void serialize(final List<T> list, final ByteBuf nlriByteBuf) {
        for (final Iterator<T> it = list.iterator(); it.hasNext(); ) {
            final T operand = it.next();
            serialize(operand.getOp(), 1, !it.hasNext(), nlriByteBuf);
            ByteBufUtils.write(nlriByteBuf, operand.getValue());
        }
    }

    @Override
    protected <T extends NumericOneByteValue> Uint8 getValue(final T item) {
        return item.getValue();
    }

    @Override
    <T extends NumericOneByteValue> NumericOperand getOp(final T item) {
        return item.getOp();
    }
}
