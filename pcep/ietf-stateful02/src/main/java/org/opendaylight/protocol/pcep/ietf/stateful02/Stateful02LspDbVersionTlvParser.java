/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedLong;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Parser for {@link LspDbVersion}
 */
@Deprecated
public final class Stateful02LspDbVersionTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 23;

    private static final int CONTENT_LENGTH = Long.SIZE / Byte.SIZE;

    @Override
    public LspDbVersion parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        return new LspDbVersionBuilder().setVersion(BigInteger.valueOf(buffer.readLong())).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof LspDbVersion, "LspDbVersionTlv is mandatory.");
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        final LspDbVersion ldvTlv = (LspDbVersion) tlv;
        Preconditions.checkArgument(ldvTlv.getVersion() != null, "Version is mandatory.");
        writeUnsignedLong(ldvTlv.getVersion(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
