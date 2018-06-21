/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class NodeDescriptorTlvParser extends AbstractLocalNodeDescriptorTlvCodec<NodeDescriptors> {
    @Override
    public void serializeTlvBody(final NodeDescriptors tlv, final ByteBuf body) {
        serializeNodeDescriptor(tlv, body);
    }

    @Override
    public QName getTlvQName() {
        return NodeDescriptors.QNAME;
    }
}
