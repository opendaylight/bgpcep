/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XROSubobjectListParser extends AbstractRSVPObjectParser {
    private static final Logger LOG = LoggerFactory.getLogger(XROSubobjectListParser.class);
    private static final short HEADER_LENGHT = 2;
    private final XROSubobjectRegistry subobjReg;

    protected XROSubobjectListParser(final XROSubobjectRegistry subobjReg) {
        this.subobjReg = Preconditions.checkNotNull(subobjReg);
    }

    public List<SubobjectContainer> parseList(final ByteBuf byteBuf) throws RSVPParsingException {
        final List<SubobjectContainer> subs = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final boolean mandatory = ((byteBuf.getUnsignedByte(byteBuf.readerIndex()) & (1 << Values.FIRST_BIT_OFFSET)) != 0) ? true : false;
            final int type = UnsignedBytes.checkedCast((byteBuf.readUnsignedByte() & Values.BYTE_MAX_VALUE_BYTES) & ~(1 << Values.FIRST_BIT_OFFSET));
            final int length = byteBuf.readUnsignedByte() - HEADER_LENGHT;
            if (length > byteBuf.readableBytes()) {
                throw new RSVPParsingException("Wrong length specified. Passed: " + length + "; Expected: <= " + byteBuf.readableBytes());
            }
            LOG.debug("Attempt to parse subobject from bytes: {}", ByteBufUtil.hexDump(byteBuf));
            final SubobjectContainer sub = this.subobjReg.parseSubobject(type, byteBuf.readSlice(length), mandatory);

            if (sub == null) {
                LOG.warn("Unknown subobject type: {}. Ignoring subobject.", type);
            } else {
                LOG.debug("Subobject was parsed. {}", sub);
                subs.add(sub);
            }
        }
        return subs;
    }

    public void serializeList(final List<SubobjectContainer> subobjects, final ByteBuf buffer) {
        Preconditions.checkArgument(subobjects != null && !subobjects.isEmpty(), "XRO must contain at least one subobject.");
        for (final SubobjectContainer subobject : subobjects) {
            this.subobjReg.serializeSubobject(subobject, buffer);
        }
    }
}
