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
import java.nio.charset.StandardCharsets;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.tlv.StringTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.tlv.StringTlvBuilder;

public class StringTlvHandler implements BmpTlvParser, BmpTlvSerializer {

    public static final int TYPE = 0;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf output) {
        Preconditions.checkArgument(tlv instanceof StringTlv, "StringTlv is mandatory.");
        TlvUtil.formatTlvUtf8(TYPE, ((StringTlv) tlv).getStringInfo(), output);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws BmpDeserializationException {
        if (buffer == null) {
            return null;
        }
        return new StringTlvBuilder().setStringInfo(buffer.toString(StandardCharsets.UTF_8)).build();
    }

}
