/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.Subobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractXROWithSubobjectsParser extends CommonObjectParser implements ObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractXROWithSubobjectsParser.class);

    private static final int HEADER_LENGTH = 2;

    private final XROSubobjectRegistry subobjReg;

    protected AbstractXROWithSubobjectsParser(final XROSubobjectRegistry subobjReg,
        final int objectClass, final int objectType) {
        super(objectClass, objectType);
        this.subobjReg = requireNonNull(subobjReg);
    }

    protected List<Subobject> parseSubobjects(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        final List<Subobject> subs = new ArrayList<>();
        while (buffer.isReadable()) {
            final boolean mandatory =
                ((buffer.getUnsignedByte(buffer.readerIndex()) & (1 << Values.FIRST_BIT_OFFSET)) != 0) ? true : false;
            final int type = UnsignedBytes.checkedCast((buffer.readUnsignedByte() & Values.BYTE_MAX_VALUE_BYTES)
                & ~(1 << Values.FIRST_BIT_OFFSET));
            final int length = buffer.readUnsignedByte() - HEADER_LENGTH;
            if (length > buffer.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                        + buffer.readableBytes());
            }
            LOG.debug("Attempt to parse subobject from bytes: {}", ByteBufUtil.hexDump(buffer));
            final Subobject sub = this.subobjReg.parseSubobject(type, buffer.readSlice(length), mandatory);
            if (sub == null) {
                LOG.warn("Parsing failed for subobject type: {}. Ignoring subobject.", type);
            } else {
                LOG.debug("Subobject was parsed. {}", sub);
                subs.add(sub);
            }
        }
        return subs;
    }

    protected final void serializeSubobject(final List<Subobject> subobjects, final ByteBuf buffer) {
        Preconditions.checkArgument(subobjects != null && !subobjects.isEmpty(),
            "XRO must contain at least one subobject.");
        for (final Subobject subobject : subobjects) {
            this.subobjReg.serializeSubobject(subobject, buffer);
        }
    }
}
