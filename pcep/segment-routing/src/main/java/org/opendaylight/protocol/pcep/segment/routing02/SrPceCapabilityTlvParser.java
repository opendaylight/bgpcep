/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.impl.tlv.TlvUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public class SrPceCapabilityTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 26;

    private static final int MSD_LENGTH = 1;
    private static final int OFFSET = 4 - MSD_LENGTH;

    @Override
    public byte[] serializeTlv(Tlv tlv) {
        Preconditions.checkNotNull(tlv, "SrPceCapability is mandatory.");
        final SrPceCapability spcTlv = (SrPceCapability) tlv;
        return TlvUtil.formatTlv(TYPE, ByteArray.intToBytes(spcTlv.getMsd()));
    }

    @Override
    public Tlv parseTlv(ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final short msd = buffer.readerIndex(OFFSET).readUnsignedByte();
        return new SrPceCapabilityBuilder().setMsd(msd).build();
    }

}
