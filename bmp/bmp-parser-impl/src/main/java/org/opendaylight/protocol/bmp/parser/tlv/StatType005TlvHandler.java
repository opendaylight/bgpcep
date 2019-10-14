/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser.tlv;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.protocol.util.ByteBufUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.InvalidatedOriginatorIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.InvalidatedOriginatorIdTlvBuilder;

public class StatType005TlvHandler implements BmpTlvParser, BmpTlvSerializer {

    public static final int TYPE = 5;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf output) {
        Preconditions.checkArgument(tlv instanceof InvalidatedOriginatorIdTlv,
                "InvalidatedOriginatorIdTlv is mandatory.");
        TlvUtil.formatTlvCounter32(TYPE, ((InvalidatedOriginatorIdTlv) tlv).getCount(), output);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws BmpDeserializationException {
        if (buffer == null) {
            return null;
        }
        return new InvalidatedOriginatorIdTlvBuilder().setCount(new Counter32(ByteBufUtils.readUint32(buffer))).build();
    }

}
