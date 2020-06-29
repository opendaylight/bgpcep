/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link LspErrorCode}.
 */
public final class Stateful07LspUpdateErrorTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 20;

    @Override
    public LspErrorCode parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        return new LspErrorCodeBuilder().setErrorCode(ByteBufUtils.readUint32(buffer)).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof LspErrorCode, "LspErrorCodeTlv is mandatory.");
        final ByteBuf body = Unpooled.buffer(Integer.BYTES);
        ByteBufUtils.writeOrZero(body, ((LspErrorCode) tlv).getErrorCode());
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
