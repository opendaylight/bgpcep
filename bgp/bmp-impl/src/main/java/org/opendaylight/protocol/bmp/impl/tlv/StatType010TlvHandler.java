/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.tlv;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.PerAfiSafiLocRibTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.stat.tlvs.PerAfiSafiLocRibTlvBuilder;

public class StatType010TlvHandler implements BmpTlvParser, BmpTlvSerializer {

    public static final int TYPE = 10;
    private final AddressFamilyRegistry afiRegistry;
    private final SubsequentAddressFamilyRegistry safiRegistry;

    public StatType010TlvHandler(final AddressFamilyRegistry afiReg, SubsequentAddressFamilyRegistry safiReg) {
        this.afiRegistry = Preconditions.checkNotNull(afiReg, "AddressFamily cannot be null");
        this.safiRegistry = Preconditions.checkNotNull(safiReg, "SubsequentAddressFamily cannot be null");
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf output) {
        Preconditions.checkArgument(tlv instanceof PerAfiSafiLocRibTlv, "PerAfiSafiLocRibInTlv is mandatory.");
        final ByteBuf buffer = Unpooled.buffer();
        ByteBufWriteUtil.writeUnsignedShort(this.afiRegistry.numberForClass(((PerAfiSafiLocRibTlv) tlv).getAfi()), buffer);
        ByteBufWriteUtil.writeUnsignedByte(this.safiRegistry.numberForClass(((PerAfiSafiLocRibTlv) tlv).getSafi()).shortValue(), buffer);
        ByteBufWriteUtil.writeUnsignedLong(((PerAfiSafiLocRibTlv) tlv).getCount().getValue(), buffer);
        TlvUtil.formatTlv(TYPE, buffer, output);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws BmpDeserializationException {
        if (buffer == null) {
            return null;
        }
        return new PerAfiSafiLocRibTlvBuilder()
                       .setAfi(this.afiRegistry.classForFamily(buffer.readUnsignedShort()))
                       .setSafi(this.safiRegistry.classForFamily(buffer.readUnsignedByte()))
                       .setCount(new Gauge64(new BigInteger(ByteArray.readAllBytes(buffer)))).build();
    }
}
