/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.AdjRibsInRoutesTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.stat.tlvs.AdjRibsInRoutesTlvBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class StatType007TlvHandler implements BmpTlvParser, BmpTlvSerializer {
    public static final int TYPE = 7;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf output) {
        checkArgument(tlv instanceof AdjRibsInRoutesTlv, "AdjRibsInRoutesTlv is mandatory.");
        TlvUtil.formatTlvGauge64(TYPE, ((AdjRibsInRoutesTlv) tlv).getCount(), output);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws BmpDeserializationException {
        if (buffer == null) {
            return null;
        }
        return new AdjRibsInRoutesTlvBuilder().setCount(new Gauge64(ByteBufUtils.readUint64(buffer))).build();
    }
}
