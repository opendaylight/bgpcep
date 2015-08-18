/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.subobjects;


import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EROSubobjectListParser extends AbstractRSVPObjectParser {
    private static final Logger LOG = LoggerFactory.getLogger(EROSubobjectListParser.class);
    private static final int HEADER_LENGTH = 2;
    private final EROSubobjectRegistry subobjReg;

    public EROSubobjectListParser(final EROSubobjectRegistry subobjReg) {
        this.subobjReg = Preconditions.checkNotNull(subobjReg);
    }

    public List<SubobjectContainer> parseList(final ByteBuf buffer) throws RSVPParsingException {
        // Explicit approval of empty ERO
        Preconditions.checkArgument(buffer != null, "Array of bytes is mandatory. Can't be null.");
        final List<SubobjectContainer> subs = new ArrayList<>();
        while (buffer.isReadable()) {
            final boolean loose = ((buffer.getUnsignedByte(buffer.readerIndex()) & (1 << Values.FIRST_BIT_OFFSET)) != 0) ? true : false;
            final int type = (buffer.readUnsignedByte() & Values.BYTE_MAX_VALUE_BYTES) & ~(1 << Values.FIRST_BIT_OFFSET);
            final int length = buffer.readUnsignedByte() - HEADER_LENGTH;
            if (length > buffer.readableBytes()) {
                throw new RSVPParsingException("Wrong length specified. Passed: " + length + "; Expected: <= "
                    + buffer.readableBytes());
            }
            LOG.debug("Attempt to parse subobject from bytes: {}", ByteBufUtil.hexDump(buffer));
            final SubobjectContainer sub = this.subobjReg.parseSubobject(type, buffer.readSlice(length), loose);
            if (sub == null) {
                LOG.warn("Unknown subobject type: {}. Ignoring subobject.", type);
            } else {
                LOG.debug("Subobject was parsed. {}", sub);
                subs.add(sub);
            }
        }
        return subs;
    }

    public final void serializeList(final List<SubobjectContainer> subobjects, final ByteBuf buffer) {
        Preconditions.checkArgument(subobjects != null && !subobjects.isEmpty(), "RRO must contain at least one subobject.");
        for (final SubobjectContainer subobject : subobjects) {
            this.subobjReg.serializeSubobject(subobject, buffer);
        }
    }
}
