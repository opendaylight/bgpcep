/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.p2mp.te.lsp;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.p2mp.pce.capability.tlv.P2mpPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.p2mp.pce.capability.tlv.P2mpPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Tlv;

public final class P2MPTeLspCapabilityParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 6;
    private static final int CONTENT_LENGTH = 2;
    @VisibleForTesting
    static final P2mpPceCapability P2MP_CAPABILITY = new P2mpPceCapabilityBuilder().build();

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof P2mpPceCapability, "P2mpPceCapability is mandatory.");
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        body.writeShort(0);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        buffer.skipBytes(CONTENT_LENGTH);
        return P2MP_CAPABILITY;
    }
}
