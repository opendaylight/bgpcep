/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.NumericOneByteOperandParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yangtools.concepts.Builder;

public final class FlowspecTypeListParser {

    public static <T> List<T> parseFlowspecTypeList(Builder<T> b, final ByteBuf nlri) {
        final List<T> oneByte = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        while (!end) {
            final byte bt = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(bt);
                //((ProtocolIpsBuilder) b).setOp(op);
                //((ProtocolIpsBuilder) b).setValue(nlri.readUnsignedByte());
            oneByte.add(b.build());
            end = op.isEndOfList();
        }
        return oneByte;
    }
}
