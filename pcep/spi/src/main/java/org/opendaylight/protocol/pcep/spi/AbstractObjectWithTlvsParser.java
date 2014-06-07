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

import java.util.Arrays;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractObjectWithTlvsParser<T> implements ObjectParser, ObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractObjectWithTlvsParser.class);

    private static final int TLV_TYPE_F_LENGTH = 2;
    private static final int TLV_LENGTH_F_LENGTH = 2;
    private static final int TLV_HEADER_LENGTH = TLV_LENGTH_F_LENGTH + TLV_TYPE_F_LENGTH;

    protected static final int PADDED_TO = 4;

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
            LOG.trace("Attempt to parse tlv from bytes: {}", ByteBufUtil.hexDump(tlvBytes));
            final Tlv tlv = this.tlvReg.parseTlv(type, tlvBytes);
            LOG.trace("Tlv was parsed. {}", tlv);
            addTlv(builder, tlv);
            bytes.readerIndex(bytes.readerIndex() + length + getPadding(TLV_HEADER_LENGTH + length, PADDED_TO));
        }
    }

    protected final byte[] serializeTlv(final Tlv tlv) {
        Preconditions.checkNotNull(tlv, "PCEP TLV is mandatory.");
        LOG.trace("Serializing PCEP TLV {}", tlv);
        final byte[] ret = this.tlvReg.serializeTlv(tlv);
        if (ret == null) {
            LOG.warn("TLV serializer for type {} could not be found.", tlv);
        }
        LOG.trace("Serialized PCEP TLV {}.", Arrays.toString(ret));
        return ret;
    }

    protected void addTlv(final T builder, final Tlv tlv) {
        // FIXME: No TLVs by default, fallback to augments
    }

    public static int getPadding(final int length, final int padding) {
        return (padding - (length % padding)) % padding;
    }
}
