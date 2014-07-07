/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.lsp.setup.type01;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBoolean;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public class PathSetupTypeTlvParser implements TlvParser, TlvSerializer {

    // http://tools.ietf.org/html/draft-sivabalan-pce-segment-routing-01#section-9.3
    public static final int TYPE = 27;
    private static final int PST_LENGTH = 1;
    private static final int OFFSET = 4 - PST_LENGTH;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv != null && tlv instanceof PathSetupType, "PathSetupType is mandatory.");
        final PathSetupType pstTlv = (PathSetupType) tlv;
        ByteBuf body = Unpooled.buffer(OFFSET + PST_LENGTH);
        body.writeZero(OFFSET);
        writeBoolean(pstTlv.isPst(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final boolean pst = buffer.readerIndex(OFFSET).readBoolean();
        return new PathSetupTypeBuilder().setPst(pst).build();
    }

}
