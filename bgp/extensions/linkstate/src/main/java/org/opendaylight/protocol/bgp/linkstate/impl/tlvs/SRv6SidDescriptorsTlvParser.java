/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6SidDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.srv6.sid._case.Srv6SidDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Srv6Sid;
import org.opendaylight.yangtools.yang.common.QName;

public final class SRv6SidDescriptorsTlvParser implements LinkstateTlvParser<Srv6SidDescriptors>,
        LinkstateTlvParser.LinkstateTlvSerializer<Srv6SidDescriptors> {

    private static final int SRV6_SID_DESCRIPTORS = 518;
    private static final int SRV6_SID_LENGTH = 16;

    @Override
    public void serializeTlvBody(final Srv6SidDescriptors tlv, final ByteBuf body) {
        body.writeBytes(tlv.getSrv6Sid().getValue());
    }

    @Override
    public Srv6SidDescriptors parseTlvBody(final ByteBuf value) {
        return new Srv6SidDescriptorsBuilder()
            .setSrv6Sid(new Srv6Sid(ByteArray.readBytes(value, SRV6_SID_LENGTH)))
            .build();
    }

    @Override
    public int getType() {
        return SRV6_SID_DESCRIPTORS;
    }

    @Override
    public QName getTlvQName() {
        return Srv6SidDescriptors.QNAME;
    }
}
