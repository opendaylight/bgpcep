/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class MultiTopoIdTlvParser implements LinkstateTlvParser<TopologyIdentifier>, LinkstateTlvParser.LinkstateTlvSerializer<TopologyIdentifier> {

    public static final QName MULTI_TOPOLOGY_ID_QNAME = QName.create(LinkDescriptors.QNAME, "multi-topology-id").intern();

    @Override
    public void serializeTlvBody(final TopologyIdentifier tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeUnsignedShort(tlv.getValue(), body);
    }

    @Override
    public int getType() {
        return TlvUtil.MULTI_TOPOLOGY_ID;
    }

    @Override
    public TopologyIdentifier parseTlvBody(final ByteBuf value) {
        return new TopologyIdentifier(value.readShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
    }

    @Override
    public QName getTlvQName() {
        return MULTI_TOPOLOGY_ID_QNAME;
    }

    public static TopologyIdentifier serializeModel(final ContainerNode prefixDesc) {
        if (prefixDesc.getChild(TlvUtil.MULTI_TOPOLOGY_NID).isPresent()) {
            return new TopologyIdentifier((Integer) prefixDesc.getChild(TlvUtil.MULTI_TOPOLOGY_NID).get().getValue());
        }
        return null;
    }
}

