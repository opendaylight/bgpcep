/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class AdvertisingNodeDescriptorTlvParser extends AbstractLocalNodeDescriptorTlvCodec<AdvertisingNodeDescriptors> {
    @Override
    public void serializeTlvBody(final AdvertisingNodeDescriptors tlv, final ByteBuf body) {
        serializeNodeDescriptor(tlv, body);
    }

    @Override
    public QName getTlvQName() {
        return AdvertisingNodeDescriptors.QNAME;
    }

}
