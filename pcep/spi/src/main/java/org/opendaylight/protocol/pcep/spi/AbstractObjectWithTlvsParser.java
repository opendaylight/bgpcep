/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractObjectWithTlvsParser<T> implements ObjectParser, ObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractObjectWithTlvsParser.class);

    private final TlvRegistry tlvReg;

    protected AbstractObjectWithTlvsParser(final TlvRegistry tlvReg) {
        this.tlvReg = Preconditions.checkNotNull(tlvReg);
    }

    protected final void parseTlvs(final T builder, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null, "Array of bytes is mandatory. Can't be null.");
        if (!bytes.isReadable()) {
            return;
        }
        while (bytes.isReadable()) {
            int type = bytes.readUnsignedShort();
            int length = bytes.readUnsignedShort();
            if (length > bytes.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= " + bytes.readableBytes()
                    + ".");
            }
            final ByteBuf tlvBytes = bytes.slice(bytes.readerIndex(), length);
            LOG.trace("Parsing PCEP TLV : {}", ByteBufUtil.hexDump(tlvBytes));
            final Tlv tlv = this.tlvReg.parseTlv(type, tlvBytes);
            LOG.trace("Parsed PCEP TLV {}.", tlv);
            addTlv(builder, tlv);
            bytes.skipBytes(length + TlvUtil.getPadding(TlvUtil.HEADER_SIZE + length, TlvUtil.PADDED_TO));
        }
    }

    protected final void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkNotNull(tlv, "PCEP TLV is mandatory.");
        LOG.trace("Serializing PCEP TLV {}", tlv);
        this.tlvReg.serializeTlv(tlv, buffer);
        LOG.trace("Serialized PCEP TLV : {}.", ByteBufUtil.hexDump(buffer));
    }

    protected void addTlv(final T builder, final Tlv tlv) {
        // FIXME: No TLVs by default, fallback to augments
    }
}
