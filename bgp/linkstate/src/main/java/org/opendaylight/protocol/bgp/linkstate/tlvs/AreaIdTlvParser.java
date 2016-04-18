/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AreaIdTlvParser implements LinkstateTlvParser<AreaIdentifier>, LinkstateTlvSerializer<AreaIdentifier> {

    private static final Logger LOG = LoggerFactory.getLogger(AreaIdTlvParser.class);

    public static final int AREA_ID = 514;

    public static final QName AREA_ID_QNAME = QName.create(NodeDescriptors.QNAME, "area-id").intern();

    @Override
    public void serializeTlvBody(AreaIdentifier tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(AREA_ID, Unpooled.copyInt(UnsignedInteger.valueOf(tlv.getValue()).intValue()), body);
        }
    }

    @Override
    public AreaIdentifier parseTlvBody(ByteBuf value) throws BGPParsingException {
        final AreaIdentifier ai = new AreaIdentifier(value.readUnsignedInt());
        LOG.debug("Parsed area identifier {}", ai);
        return ai;
    }

}
