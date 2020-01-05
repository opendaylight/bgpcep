/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class PathSetupTypeTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 28;

    private static final int CONTENT_LENGTH = 4;
    private static final int PST_LENGTH = 1;
    private static final int OFFSET = CONTENT_LENGTH - PST_LENGTH;
    private static final short RSVP_TE_PST = 0;
    private static final String UNSUPPORTED_PST = "Unsupported path setup type.";

    private static final Set<Uint8> PSTS = new HashSet<>();

    public PathSetupTypeTlvParser() {
        PSTS.add(Uint8.valueOf(RSVP_TE_PST));
    }

    public PathSetupTypeTlvParser(final short srTePst) {
        this();
        PSTS.add(Uint8.valueOf(srTePst));
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof PathSetupType, "PathSetupType is mandatory.");
        final PathSetupType pstTlv = (PathSetupType) tlv;
        checkArgument(checkPST(pstTlv.getPst()), UNSUPPORTED_PST);
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        body.writeZero(OFFSET);
        ByteBufUtils.writeOrZero(body, pstTlv.getPst());
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final Uint8 pst = ByteBufUtils.readUint8(buffer.readerIndex(OFFSET));
        if (!checkPST(pst)) {
            throw new PCEPDeserializerException(UNSUPPORTED_PST);
        }
        return new PathSetupTypeBuilder().setPst(pst).build();
    }

    private static boolean checkPST(final Uint8 pst) {
        return pst != null && PSTS.contains(pst);
    }
}
