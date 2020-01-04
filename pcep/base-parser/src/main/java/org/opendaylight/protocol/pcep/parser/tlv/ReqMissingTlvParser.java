/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser {@link ReqMissing}.
 */
public class ReqMissingTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 3;

    @Override
    public ReqMissing parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        return new ReqMissingBuilder().setRequestId(new RequestId(ByteBufUtils.readUint32(buffer))).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof ReqMissing, "ReqMissingTlv is mandatory.");
        final ReqMissing req = (ReqMissing) tlv;
        final ByteBuf body = Unpooled.buffer();
        Preconditions.checkArgument(req.getRequestId() != null, "RequestId is mandatory.");
        writeUnsignedInt(req.getRequestId().getValue(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
