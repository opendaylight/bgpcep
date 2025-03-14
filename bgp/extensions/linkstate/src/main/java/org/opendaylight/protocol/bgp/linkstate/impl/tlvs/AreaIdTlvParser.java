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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class AreaIdTlvParser implements LinkstateTlvParser<AreaIdentifier>,
        LinkstateTlvParser.LinkstateTlvSerializer<AreaIdentifier> {
    private static final int AREA_ID = 514;

    public static final QName AREA_ID_QNAME = QName.create(NodeDescriptors.QNAME, "area-id").intern();

    @Override
    public void serializeTlvBody(final AreaIdentifier tlv, final ByteBuf body) {
        ByteBufUtils.write(body, tlv.getValue());
    }

    @Override
    public AreaIdentifier parseTlvBody(final ByteBuf value) {
        return new AreaIdentifier(ByteBufUtils.readUint32(value));
    }

    @Override
    public int getType() {
        return AREA_ID;
    }

    @Override
    public QName getTlvQName() {
        return AREA_ID_QNAME;
    }
}
