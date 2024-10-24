/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.attributes.Srv6EndpointBehavior;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.attributes.Srv6EndpointBehaviorBuilder;
import org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.common.QName;

public final class SRv6EndpointTlvParser implements LinkstateTlvParser<Srv6EndpointBehavior>,
        LinkstateTlvParser.LinkstateTlvSerializer<Srv6EndpointBehavior> {

    private static final int SRV6_ENDPOINT_BEHAVIOR = 1250;
    private static final int FLAGS_SIZE = 1;

    public static final QName SRV6_ENDPOINT_BEHAVIOR_QNAME = YangModuleInfoImpl.qnameOf("srv6-endpoint-behavior");

    @Override
    public void serializeTlvBody(final Srv6EndpointBehavior tlv, final ByteBuf body) {
        writeUint16(body, tlv.getEndpointBehavior());
        // No Flags has been defined in RFC9514 (https://www.rfc-editor.org/rfc/rfc9514.html#section-7.1)
        body.writeZero(FLAGS_SIZE);
        writeUint8(body, tlv.getAlgo());
    }

    @Override
    public Srv6EndpointBehavior parseTlvBody(final ByteBuf value) {
        final Srv6EndpointBehaviorBuilder builder = new Srv6EndpointBehaviorBuilder();

        builder.setEndpointBehavior(readUint16(value));
        // No Flags has been defined in RFC9514 (https://www.rfc-editor.org/rfc/rfc9514.html#section-7.1)
        value.skipBytes(FLAGS_SIZE);
        builder.setAlgo(readUint8(value));
        return builder.build();
    }

    @Override
    public int getType() {
        return SRV6_ENDPOINT_BEHAVIOR;
    }

    @Override
    public QName getTlvQName() {
        return SRV6_ENDPOINT_BEHAVIOR_QNAME;
    }
}
