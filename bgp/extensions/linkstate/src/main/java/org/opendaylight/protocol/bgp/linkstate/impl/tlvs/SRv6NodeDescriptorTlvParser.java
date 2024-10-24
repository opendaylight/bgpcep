/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.srv6.sid._case.Srv6NodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public class SRv6NodeDescriptorTlvParser extends
    AbstractLocalNodeDescriptorTlvCodec<Srv6NodeDescriptors> {

    @Override
    public void serializeTlvBody(final Srv6NodeDescriptors tlv, final ByteBuf body) {
        serializeNodeDescriptor(tlv, body);
    }

    @Override
    public QName getTlvQName() {
        return Srv6NodeDescriptors.QNAME;
    }
}
