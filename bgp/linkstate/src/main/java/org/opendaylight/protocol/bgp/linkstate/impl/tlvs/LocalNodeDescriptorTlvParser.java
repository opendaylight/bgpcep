/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class LocalNodeDescriptorTlvParser extends AbstractLocalNodeDescriptorTlvCodec<LocalNodeDescriptors> {
    @Override
    public void serializeTlvBody(final LocalNodeDescriptors tlv, final ByteBuf body) {
        serializeNodeDescriptor(tlv, body);
        final SimpleNlriTypeRegistry tlvReg = SimpleNlriTypeRegistry.getInstance();
        tlvReg.serializeTlv(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME, tlv.getBgpRouterId(), body);
        tlvReg.serializeTlv(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME, tlv.getMemberAsn(), body);
    }

    @Override
    public QName getTlvQName() {
        return LocalNodeDescriptors.QNAME;
    }

}
