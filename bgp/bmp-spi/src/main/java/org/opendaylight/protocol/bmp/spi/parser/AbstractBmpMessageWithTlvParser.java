/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yangtools.concepts.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract  class AbstractBmpMessageWithTlvParser<T extends Builder<?>> extends AbstractBmpMessageParser {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBmpMessageWithTlvParser.class);

    private final BmpTlvRegistry tlvRegistry;

    public AbstractBmpMessageWithTlvParser(final BmpTlvRegistry tlvRegistry) {
        this.tlvRegistry = tlvRegistry;
    }

    protected final void parseTlvs(final T builder, final ByteBuf bytes) throws BmpDeserializationException {
        Preconditions.checkArgument(bytes != null, "Array of bytes is mandatory. Can't be null.");
        if (!bytes.isReadable()) {
            return;
        }
        while (bytes.isReadable()) {
            final int type = bytes.readUnsignedShort();
            final int length = bytes.readUnsignedShort();
            if (length > bytes.readableBytes()) {
                throw new BmpDeserializationException("Wrong length specified. Passed: " + length + "; Expected: <= " + bytes.readableBytes()
                    + ".");
            }
            final ByteBuf tlvBytes = bytes.readSlice(length);
            LOG.trace("Parsing BMP TLV : {}", ByteBufUtil.hexDump(tlvBytes));

            final Tlv tlv = this.tlvRegistry.parseTlv(type, tlvBytes);
            if(tlv != null) {
                LOG.trace("Parsed BMP TLV {}.", tlv);
                addTlv(builder, tlv);
            }
        }
    }

    protected final void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        requireNonNull(tlv, "BMP TLV is mandatory.");
        LOG.trace("Serializing BMP TLV {}", tlv);
        this.tlvRegistry.serializeTlv(tlv, buffer);
        LOG.trace("Serialized BMP TLV : {}.", ByteBufUtil.hexDump(buffer));
    }

    protected void addTlv(final T builder, final Tlv tlv) {
        //no TLVs by default
    }

}
