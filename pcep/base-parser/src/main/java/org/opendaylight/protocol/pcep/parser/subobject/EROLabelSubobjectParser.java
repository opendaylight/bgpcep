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
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.LabelRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.label._case.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.label._case.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;

public class EROLabelSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 3;

    private static final int FLAGS_SIZE = 8;

    private static final int C_TYPE_F_LENGTH = 1;

    private static final int C_TYPE_F_OFFSET = FLAGS_SIZE / Byte.SIZE;

    private static final int HEADER_LENGTH = C_TYPE_F_OFFSET + C_TYPE_F_LENGTH;

    private static final int U_FLAG_OFFSET = 0;

    private final LabelRegistry registry;

    public EROLabelSubobjectParser(final LabelRegistry labelReg) {
        this.registry = requireNonNull(labelReg);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() < HEADER_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                    + HEADER_LENGTH + ".");
        }
        final BitArray reserved = BitArray.valueOf(buffer, FLAGS_SIZE);
        final short cType = buffer.readUnsignedByte();

        final LabelType labelType = this.registry.parseLabel(cType, buffer.slice());
        if (labelType == null) {
            throw new PCEPDeserializerException("Unknown C-TYPE for ero label subobject. Passed: " + cType);
        }
        final LabelBuilder builder = new LabelBuilder();
        builder.setUniDirectional(reserved.get(U_FLAG_OFFSET));
        builder.setLabelType(labelType);
        return new SubobjectBuilder().setLoose(loose).setSubobjectType(new LabelCaseBuilder().setLabel(builder.build()).build()).build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof LabelCase, "Unknown subobject instance. Passed %s. Needed LabelCase.", subobject.getSubobjectType().getClass());
        final Label label = ((LabelCase) subobject.getSubobjectType()).getLabel();
        final ByteBuf body = Unpooled.buffer();
        this.registry.serializeLabel(label.isUniDirectional(), false, label.getLabelType(), body);
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }
}
