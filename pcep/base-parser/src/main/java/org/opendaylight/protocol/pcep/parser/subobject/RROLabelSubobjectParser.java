/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.LabelRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.label._case.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.label._case.LabelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RROLabelSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(RROLabelSubobjectParser.class);

    public static final int TYPE = 3;

    public static final int FLAGS_SIZE = 8;

    public static final int C_TYPE_F_LENGTH = 1;

    public static final int C_TYPE_F_OFFSET = FLAGS_SIZE / Byte.SIZE;

    public static final int HEADER_LENGTH = C_TYPE_F_OFFSET + C_TYPE_F_LENGTH;

    public static final int U_FLAG_OFFSET = 0;
    public static final int G_FLAG_OFFSET = 7;

    private final LabelRegistry registry;

    public RROLabelSubobjectParser(final LabelRegistry labelReg) {
        this.registry = requireNonNull(labelReg);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() < HEADER_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                    + HEADER_LENGTH + ".");
        }
        final BitArray reserved = BitArray.valueOf(buffer, FLAGS_SIZE);

        final short cType = buffer.readUnsignedByte();

        final LabelType labelType = this.registry.parseLabel(cType, buffer.slice());
        if (labelType == null) {
            LOG.warn("Ignoring RRO label subobject with unknown C-TYPE: {}", cType);
            return null;
        }
        final LabelBuilder builder = new LabelBuilder();
        builder.setUniDirectional(reserved.get(U_FLAG_OFFSET));
        builder.setGlobal(reserved.get(G_FLAG_OFFSET));
        builder.setLabelType(labelType);
        return new SubobjectBuilder().setSubobjectType(new LabelCaseBuilder().setLabel(builder.build()).build()).build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        requireNonNull(subobject.getSubobjectType(), "Subobject type cannot be empty.");
        final Label label = ((LabelCase) subobject.getSubobjectType()).getLabel();
        final ByteBuf body = Unpooled.buffer();
        this.registry.serializeLabel(label.isUniDirectional(), label.isGlobal(), label.getLabelType(), body);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}
