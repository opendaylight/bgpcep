/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;

/**
 * Common parent class for numeric byte operands.
 *
 * @param <N> numeric operand type
 */
abstract class AbstractNumericByteOperandParser<N, V extends Number> extends AbstractNumericOperandParser<N> {

    @Override
    public final <T extends N> String toString(final List<T> list) {
        final StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (final T item : list) {
            buffer.append(super.toString(getOp(item), isFirst));
            buffer.append(getValue(item));
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    abstract <T extends N> V getValue(final T item);

    abstract <T extends N> NumericOperand getOp(final T item);
}