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
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RROSubobjectListParser extends AbstractRSVPObjectParser {
    private static final Logger LOG = LoggerFactory.getLogger(RROSubobjectListParser.class);
    private static final int HEADER_LENGTH = 2;
    private final RROSubobjectRegistry subobjReg;

    public RROSubobjectListParser(final RROSubobjectRegistry subobjReg) {
        this.subobjReg = Preconditions.checkNotNull(subobjReg);
    }

    public List<SubobjectContainer> parseList(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final List<SubobjectContainer> subs = new ArrayList<>();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedByte();
            final int length = buffer.readUnsignedByte() - HEADER_LENGTH;
            if (length > buffer.readableBytes()) {
                throw new RSVPParsingException("Wrong length specified. Passed: " + length + "; Expected: <= "
                    + buffer.readableBytes());
            }
            LOG.debug("Attempt to parse subobject from bytes: {}", ByteBufUtil.hexDump(buffer));
            final SubobjectContainer sub = this.subobjReg.parseSubobject(type, buffer.readSlice(length));
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
