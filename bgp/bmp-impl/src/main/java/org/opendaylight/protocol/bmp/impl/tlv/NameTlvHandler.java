/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.tlv;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.name.tlv.NameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.name.tlv.NameTlvBuilder;

public class NameTlvHandler implements BmpTlvParser, BmpTlvSerializer {

    public static final int TYPE = 2;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf output) {
        Preconditions.checkArgument(tlv instanceof NameTlv, "NameTlv is mandatory.");
        TlvUtil.formatTlvAscii(TYPE, ((NameTlv) tlv).getName(), output);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws BmpDeserializationException {
        if (buffer == null) {
            return null;
        }
        return new NameTlvBuilder().setName(buffer.toString(Charsets.US_ASCII)).build();
    }

}
