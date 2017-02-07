/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnParser;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnSerializer;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEsiTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

abstract class AbstractEvpnNlri implements EvpnParser, EvpnSerializer {

    static final int MAC_ADDRESS_LENGTH = 6;
    static final int ESI_SIZE = 10;
    private static final NodeIdentifier ESI_NID = NodeIdentifier.create(Esi.QNAME);
    static final int ZERO_BYTE = 1;

    @Override
    public final ByteBuf serializeEvpn(final EvpnChoice evpn, final ByteBuf common) {
        final ByteBuf output = Unpooled.buffer();
        final ByteBuf body = serializeBody(evpn);
        output.writeByte(getType().getIntValue());
        output.writeByte(body.readableBytes() + common.readableBytes());
        output.writeBytes(common);
        output.writeBytes(body);
        return output;
    }

    protected abstract NlriType getType();

    protected abstract ByteBuf serializeBody(final EvpnChoice evpn);

    protected static Esi serializeEsi(final ContainerNode evpn) {
        return SimpleEsiTypeRegistry.getInstance().parseEsiModel((ChoiceNode) evpn.getChild(ESI_NID).get());
    }
}
