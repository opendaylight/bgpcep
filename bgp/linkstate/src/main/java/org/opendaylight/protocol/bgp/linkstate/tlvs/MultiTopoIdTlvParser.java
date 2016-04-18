/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MultiTopoIdTlvParser implements LinkstateTlvParser<TopologyIdentifier>, LinkstateTlvSerializer<TopologyIdentifier> {

    private static final Logger LOG = LoggerFactory.getLogger(MultiTopoIdTlvParser.class);

    public static final QName MULTI_TOPOLOGY_ID_QNAME = QName.create(LinkDescriptors.QNAME, "multi-topology-id").intern();

    @Override
    public void serializeTlvBody(TopologyIdentifier tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(tlv.getValue()), body);
        }
    }

    @Override
    public TopologyIdentifier parseTlvBody(ByteBuf value) throws BGPParsingException {
        final TopologyIdentifier topologyId = new TopologyIdentifier(value.readShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Parsed Topology Identifier: {}", topologyId);
        }
        return topologyId;
    }
}

