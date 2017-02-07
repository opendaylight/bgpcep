/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.evpn.spi.EsiParser;
import org.opendaylight.protocol.bgp.evpn.spi.EsiSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;

abstract class AbstractEsiType implements EsiParser, EsiSerializer {
    private static final int BODY_LENGTH = 9;
    static final int ZERO_BYTE = 1;
    static final int MAC_ADDRESS_LENGTH = 6;

    @Override
    public final void serializeEsi(final Esi esi, final ByteBuf buffer) {
        final ByteBuf body = Unpooled.buffer(BODY_LENGTH);
        serializeBody(esi, body);
        buffer.writeByte(getType().getIntValue());
        buffer.writeBytes(body);
    }

    protected abstract void serializeBody(final Esi esi, final ByteBuf buffer);

    protected abstract EsiType getType();
}
